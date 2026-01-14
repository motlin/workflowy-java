package com.workflowy.data.converter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gs.fw.common.mithra.finder.Operation;
import com.gs.fw.common.mithra.list.merge.TopLevelMergeOptions;
import com.workflowy.DataImportTimestamp;
import com.workflowy.DataImportTimestampFinder;
import com.workflowy.Mirror;
import com.workflowy.MirrorFinder;
import com.workflowy.MirrorList;
import com.workflowy.NodeContent;
import com.workflowy.NodeContentFinder;
import com.workflowy.NodeContentList;
import com.workflowy.NodeDate;
import com.workflowy.NodeDateFinder;
import com.workflowy.NodeDateList;
import com.workflowy.NodeMetadata;
import com.workflowy.NodeMetadataFinder;
import com.workflowy.NodeMetadataList;
import com.workflowy.NodeS3File;
import com.workflowy.NodeS3FileFinder;
import com.workflowy.NodeS3FileList;
import com.workflowy.NodeTagMapping;
import com.workflowy.NodeTagMappingFinder;
import com.workflowy.NodeTagMappingList;
import com.workflowy.Tag;
import com.workflowy.TagFinder;
import com.workflowy.TagList;
import com.workflowy.User;
import com.workflowy.UserFinder;
import com.workflowy.VirtualRootMapping;
import com.workflowy.VirtualRootMappingFinder;
import com.workflowy.VirtualRootMappingList;
import com.workflowy.data.pojo.InputBacklinkMetadata;
import com.workflowy.data.pojo.InputCalendarMetadata;
import com.workflowy.data.pojo.InputItem;
import com.workflowy.data.pojo.InputMetadata;
import com.workflowy.data.pojo.InputMirrorMetadata;
import com.workflowy.data.pojo.InputS3FileMetadata;
import cool.klass.data.store.DataStore;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.eclipse.collections.impl.utility.MapIterate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorkflowyDataConverter {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowyDataConverter.class);

	private static final long WORKFLOWY_EPOCH_OFFSET = 1262304000L;
	private static final Pattern FILE_DATE_PATTERN = Pattern.compile("\\.(\\d{4}-\\d{2}-\\d{2})\\.");
	private static final Pattern FILE_EMAIL_PATTERN = Pattern.compile("^\\((.+?)\\)\\.");

	private final ObjectMapper objectMapper;
	private final DataStore dataStore;
	private final File backupFile;
	private final String userId;

	private final MutableMap<String, NodeContent> nodeContents = MapAdapter.adapt(new LinkedHashMap<>());
	private final MutableMap<String, NodeMetadata> nodeMetadatas = MapAdapter.adapt(new LinkedHashMap<>());
	private final MutableMap<String, Tag> tags = MapAdapter.adapt(new LinkedHashMap<>());
	private final NodeTagMappingList nodeTagMappings = new NodeTagMappingList();
	private final MirrorList mirrors = new MirrorList();
	private final NodeDateList nodeDates = new NodeDateList();
	private final NodeS3FileList nodeS3Files = new NodeS3FileList();
	private final VirtualRootMappingList virtualRootMappings = new VirtualRootMappingList();

	private WorkflowyDataConverter(
		@Nonnull ObjectMapper objectMapper,
		@Nonnull DataStore dataStore,
		@Nonnull File backupFile
	) {
		this.objectMapper = Objects.requireNonNull(objectMapper);
		this.dataStore = Objects.requireNonNull(dataStore);
		this.backupFile = Objects.requireNonNull(backupFile);
		this.userId = extractUserIdFromFilename(backupFile);
	}

	private static String extractUserIdFromFilename(File file) {
		String fileName = file.getName();
		Matcher matcher = FILE_EMAIL_PATTERN.matcher(fileName);
		if (matcher.find()) {
			return matcher.group(1);
		}
		throw new IllegalArgumentException("Could not extract email from filename: " + fileName);
	}

	public static void convert(
		@Nonnull Path backupsPath,
		@Nonnull ObjectMapper objectMapper,
		@Nonnull DataStore dataStore,
		int daysLimit
	) {
		Instant highWatermark = WorkflowyDataConverter.getHighWatermark();

		ImmutableList<File> filesToProcess = WorkflowyDataConverter.getBackupFiles(backupsPath)
			.selectWith(WorkflowyDataConverter::isAfterHighWatermark, highWatermark)
			.take(daysLimit);

		if (filesToProcess.isEmpty()) {
			LOGGER.info("No files to process after highWatermark {}", highWatermark);
			return;
		}

		LOGGER.info("Processing {} files after highWatermark {}", filesToProcess.size(), highWatermark);
		LOGGER.info("filesToProcess = {}", filesToProcess);

		filesToProcess
			.asLazy()
			.collect((file) -> new WorkflowyDataConverter(objectMapper, dataStore, file))
			.forEach(WorkflowyDataConverter::processBackupFile);
	}

	private void processBackupFile() {
		try {
			this.processBackupFileOrThrow();
		} catch (IOException e) {
			throw new RuntimeException("Failed to process backup file: " + this.backupFile, e);
		}
	}

	private void processBackupFileOrThrow() throws IOException {
		LOGGER.info("Processing backup file: {}", this.backupFile);

		List<InputItem> rootItems = this.objectMapper.readValue(this.backupFile, new TypeReference<>() {});

		Instant backupInstant = getFileTimestamp(this.backupFile);

		LOGGER.info("Pass 1: Creating nodes from {} root items", rootItems.size());
		this.processNodesPass1(rootItems, null, 0);
		LOGGER.info(
			"Created {} node contents and {} node metadatas",
			this.nodeContents.size(),
			this.nodeMetadatas.size()
		);

		LOGGER.info("Pass 2: Extracting tags");
		this.extractTagsFromNodes();
		LOGGER.info("Extracted {} tags and {} node-tag mappings", this.tags.size(), this.nodeTagMappings.size());

		LOGGER.info("Pass 3: Processing metadata (mirrors, dates, S3 files, virtual roots)");
		this.processMetadata(rootItems);
		LOGGER.info(
			"Created {} mirrors, {} node dates, {} S3 files, {} virtual root mappings",
			this.mirrors.size(),
			this.nodeDates.size(),
			this.nodeS3Files.size(),
			this.virtualRootMappings.size()
		);

		this.mergeIntoDatabase(backupInstant);
	}

	private void processNodesPass1(List<InputItem> inputItems, String parentId, int startPriority) {
		int priority = startPriority;
		for (InputItem inputItem : inputItems) {
			NodeContent nodeContent = this.createNodeContent(inputItem, parentId);
			NodeMetadata nodeMetadata = this.createNodeMetadata(inputItem, priority);
			this.nodeContents.put(inputItem.id(), nodeContent);
			this.nodeMetadatas.put(inputItem.id(), nodeMetadata);
			priority++;

			if (inputItem.hasChildren()) {
				this.processNodesPass1(inputItem.children(), inputItem.id(), 0);
			}
		}
	}

	private NodeContent createNodeContent(InputItem inputItem, String parentId) {
		NodeContent nodeContent = new NodeContent();
		nodeContent.setId(inputItem.id());
		nodeContent.setParentId(parentId);
		nodeContent.setName(inputItem.name() != null ? inputItem.name() : "");
		nodeContent.setNote(inputItem.note());
		return nodeContent;
	}

	private NodeMetadata createNodeMetadata(InputItem inputItem, int priority) {
		NodeMetadata nodeMetadata = new NodeMetadata();
		nodeMetadata.setNodeId(inputItem.id());
		nodeMetadata.setPriority(priority);
		nodeMetadata.setCompleted(inputItem.isCompleted());
		nodeMetadata.setCompletedAt(convertWorkflowyTimestamp(inputItem.completedTimestamp()));
		nodeMetadata.setCollapsed(false);
		nodeMetadata.setLastModified(convertWorkflowyTimestamp(inputItem.lastModifiedTimestamp()));
		nodeMetadata.setCreatedById(this.userId);
		nodeMetadata.setCreatedOn(convertWorkflowyTimestamp(inputItem.createdTimestamp()));
		nodeMetadata.setLastUpdatedById(this.userId);

		InputMetadata metadata = inputItem.metadata();
		if (metadata != null) {
			nodeMetadata.setLayoutMode(metadata.layoutMode());
			nodeMetadata.setVirtualRoot(Boolean.TRUE.equals(metadata.isVirtualRoot()));
			nodeMetadata.setReferencesRoot(Boolean.TRUE.equals(metadata.isReferencesRoot()));

			if (metadata.ai() != null) {
				nodeMetadata.setInChat(metadata.ai().inChat());
			}

			if (metadata.mirror() != null) {
				if (metadata.mirror().isMirrorRoot() != null) {
					nodeMetadata.setMirrorRoot(metadata.mirror().isMirrorRoot());
				}
				if (metadata.mirror().originalId() != null) {
					nodeMetadata.setOriginalId(metadata.mirror().originalId());
				}
			}

			// metadata.originalId takes precedence if both are present
			if (metadata.originalId() != null) {
				nodeMetadata.setOriginalId(metadata.originalId());
			}

			if (MapIterate.notEmpty(metadata.changes())) {
				try {
					nodeMetadata.setChanges(this.objectMapper.writeValueAsString(metadata.changes()));
				} catch (Exception e) {
					LOGGER.warn("Failed to serialize changes for node {}: {}", inputItem.id(), e.getMessage());
				}
			}
		}
		return nodeMetadata;
	}

	private void extractTagsFromNodes() {
		for (NodeContent nodeContent : this.nodeContents.values()) {
			this.extractTagsFromName(nodeContent);
		}
	}

	private void extractTagsFromName(NodeContent nodeContent) {
		String name = nodeContent.getName();
		if (name == null || name.isEmpty()) {
			return;
		}

		List<String> extractedTags = HashtagExtractor.extractHashtags(name);

		for (String tagName : extractedTags) {
			this.tags.computeIfAbsent(tagName, (t) -> {
					Tag newTag = new Tag();
					newTag.setName(t);
					newTag.setColor(null);
					return newTag;
				});

			NodeTagMapping mapping = new NodeTagMapping();
			mapping.setNodeId(nodeContent.getId());
			mapping.setTagName(tagName);
			this.nodeTagMappings.add(mapping);
		}
	}

	private void processMetadata(List<InputItem> inputItems) {
		for (InputItem inputItem : inputItems) {
			this.processInputItemMetadata(inputItem);
			if (inputItem.hasChildren()) {
				this.processMetadata(inputItem.children());
			}
		}
	}

	private void processInputItemMetadata(InputItem inputItem) {
		InputMetadata metadata = inputItem.metadata();
		if (metadata == null) {
			return;
		}

		if (metadata.hasMirror()) {
			this.processMirrorMetadata(inputItem.id(), metadata.mirror());
		}

		if (metadata.hasBacklink()) {
			this.processBacklinkMetadata(metadata.backlink());
		}

		if (metadata.hasCalendar()) {
			this.processCalendarMetadata(inputItem.id(), metadata.calendar());
		}

		if (metadata.s3File() != null) {
			this.processS3FileMetadata(inputItem.id(), metadata.s3File());
		}

		if (MapIterate.notEmpty(metadata.virtualRootIds())) {
			this.processVirtualRootIds(inputItem.id(), metadata.virtualRootIds());
		}
	}

	private void processMirrorMetadata(String nodeId, InputMirrorMetadata mirrorMeta) {
		for (String sourceId : mirrorMeta.getMirrorSourceIds()) {
			Mirror mirror = new Mirror();
			mirror.setId(UUID.randomUUID().toString());
			mirror.setMirrorRootId(sourceId);
			mirror.setMirrorNodeId(nodeId);
			mirror.setBacklink(false);
			this.mirrors.add(mirror);
		}

		for (String sourceId : mirrorMeta.getBacklinkMirrorIds()) {
			Mirror mirror = new Mirror();
			mirror.setId(UUID.randomUUID().toString());
			mirror.setMirrorRootId(sourceId);
			mirror.setMirrorNodeId(nodeId);
			mirror.setBacklink(true);
			this.mirrors.add(mirror);
		}
	}

	private void processBacklinkMetadata(InputBacklinkMetadata backlinkMeta) {
		if (backlinkMeta.sourceId() != null && backlinkMeta.targetId() != null) {
			Mirror mirror = new Mirror();
			mirror.setId(UUID.randomUUID().toString());
			mirror.setMirrorRootId(backlinkMeta.sourceId());
			mirror.setMirrorNodeId(backlinkMeta.targetId());
			mirror.setBacklink(true);
			this.mirrors.add(mirror);
		}
	}

	private void processCalendarMetadata(String nodeId, InputCalendarMetadata calendarMeta) {
		if (calendarMeta.date() != null) {
			Timestamp dateValue = parseCalendarDate(calendarMeta.date());
			if (dateValue != null) {
				NodeDate nodeDate = new NodeDate();
				nodeDate.setId(UUID.randomUUID().toString());
				nodeDate.setNodeId(nodeId);
				nodeDate.setDateValue(dateValue);
				nodeDate.setRoot(calendarMeta.isRoot());
				nodeDate.setLevel(calendarMeta.level());
				nodeDate.setDateId(calendarMeta.dateId());
				nodeDate.setTimestamp(calendarMeta.timestamp());
				if (calendarMeta.value() != null) {
					nodeDate.setValue(String.valueOf(calendarMeta.value()));
				}
				this.nodeDates.add(nodeDate);
			}
		}
	}

	private void processS3FileMetadata(String nodeId, InputS3FileMetadata s3FileMeta) {
		NodeS3File nodeS3File = new NodeS3File();
		nodeS3File.setId(UUID.randomUUID().toString());
		nodeS3File.setNodeId(nodeId);
		nodeS3File.setFile(s3FileMeta.isFile() != null && s3FileMeta.isFile());
		nodeS3File.setFileName(s3FileMeta.fileName());
		nodeS3File.setFileType(s3FileMeta.fileType());
		nodeS3File.setObjectFolder(s3FileMeta.objectFolder());
		if (s3FileMeta.isAnimatedGIF() != null) {
			nodeS3File.setAnimatedGIF(s3FileMeta.isAnimatedGIF());
		}
		if (s3FileMeta.imageOriginalWidth() != null) {
			nodeS3File.setImageOriginalWidth(s3FileMeta.imageOriginalWidth());
		}
		if (s3FileMeta.imageOriginalHeight() != null) {
			nodeS3File.setImageOriginalHeight(s3FileMeta.imageOriginalHeight());
		}
		if (s3FileMeta.imageOriginalPixels() != null) {
			nodeS3File.setImageOriginalPixels(s3FileMeta.imageOriginalPixels());
		}
		this.nodeS3Files.add(nodeS3File);
	}

	private void processVirtualRootIds(String nodeId, Map<String, Boolean> virtualRootIds) {
		for (String virtualRootId : virtualRootIds.keySet()) {
			VirtualRootMapping mapping = new VirtualRootMapping();
			mapping.setNodeId(nodeId);
			mapping.setVirtualRootId(virtualRootId);
			this.virtualRootMappings.add(mapping);
		}
	}

	private void ensureUserExists() {
		User existingUser = UserFinder.findOne(UserFinder.userId().eq(this.userId));
		if (existingUser == null) {
			LOGGER.info("Creating user: {}", this.userId);
			User user = new User();
			user.setUserId(this.userId);
			user.setEmail(this.userId);
			user.insert();
		}
	}

	private void mergeIntoDatabase(Instant backupInstant) {
		long time = backupInstant.toEpochMilli();

		this.dataStore.runInTransaction((transaction) -> {
				transaction.setSystemTime(time);

				this.ensureUserExists();

				LOGGER.info("Merging {} tags", this.tags.size());
				TagList existingTags = TagFinder.findMany(TagFinder.all());
				TagList updatedTags = new TagList();
				updatedTags.addAll(this.tags.values());
				TopLevelMergeOptions<Tag> tagMergeOptions = new TopLevelMergeOptions<>(TagFinder.getFinderInstance());
				existingTags.merge(updatedTags, tagMergeOptions);

				LOGGER.info("Merging {} node contents", this.nodeContents.size());
				NodeContentList existingContents = NodeContentFinder.findMany(NodeContentFinder.all());
				NodeContentList updatedContents = new NodeContentList();
				updatedContents.addAll(this.nodeContents.values());
				TopLevelMergeOptions<NodeContent> contentMergeOptions = new TopLevelMergeOptions<>(
					NodeContentFinder.getFinderInstance()
				);
				existingContents.merge(updatedContents, contentMergeOptions);

				LOGGER.info("Merging {} node metadatas", this.nodeMetadatas.size());
				NodeMetadataList existingMetadatas = NodeMetadataFinder.findMany(NodeMetadataFinder.all());
				NodeMetadataList updatedMetadatas = new NodeMetadataList();
				updatedMetadatas.addAll(this.nodeMetadatas.values());
				TopLevelMergeOptions<NodeMetadata> metadataMergeOptions = new TopLevelMergeOptions<>(
					NodeMetadataFinder.getFinderInstance()
				);
				metadataMergeOptions.doNotCompare(
					NodeMetadataFinder.createdById(),
					NodeMetadataFinder.createdOn(),
					NodeMetadataFinder.lastUpdatedById()
				);
				existingMetadatas.merge(updatedMetadatas, metadataMergeOptions);

				LOGGER.info("Merging {} node-tag mappings", this.nodeTagMappings.size());
				NodeTagMappingList existingMappings = NodeTagMappingFinder.findMany(NodeTagMappingFinder.all());
				TopLevelMergeOptions<NodeTagMapping> mappingMergeOptions = new TopLevelMergeOptions<>(
					NodeTagMappingFinder.getFinderInstance()
				);
				existingMappings.merge(this.nodeTagMappings, mappingMergeOptions);

				LOGGER.info("Merging {} mirrors", this.mirrors.size());
				MirrorList existingMirrors = MirrorFinder.findMany(MirrorFinder.all());
				TopLevelMergeOptions<Mirror> mirrorMergeOptions = new TopLevelMergeOptions<>(
					MirrorFinder.getFinderInstance()
				);
				existingMirrors.merge(this.mirrors, mirrorMergeOptions);

				LOGGER.info("Merging {} node dates", this.nodeDates.size());
				NodeDateList existingDates = NodeDateFinder.findMany(NodeDateFinder.all());
				TopLevelMergeOptions<NodeDate> dateMergeOptions = new TopLevelMergeOptions<>(
					NodeDateFinder.getFinderInstance()
				);
				existingDates.merge(this.nodeDates, dateMergeOptions);

				LOGGER.info("Merging {} node S3 files", this.nodeS3Files.size());
				NodeS3FileList existingS3Files = NodeS3FileFinder.findMany(NodeS3FileFinder.all());
				TopLevelMergeOptions<NodeS3File> s3FileMergeOptions = new TopLevelMergeOptions<>(
					NodeS3FileFinder.getFinderInstance()
				);
				existingS3Files.merge(this.nodeS3Files, s3FileMergeOptions);

				LOGGER.info("Merging {} virtual root mappings", this.virtualRootMappings.size());
				VirtualRootMappingList existingVirtualRoots = VirtualRootMappingFinder.findMany(
					VirtualRootMappingFinder.all()
				);
				TopLevelMergeOptions<VirtualRootMapping> virtualRootMergeOptions = new TopLevelMergeOptions<>(
					VirtualRootMappingFinder.getFinderInstance()
				);
				existingVirtualRoots.merge(this.virtualRootMappings, virtualRootMergeOptions);

				WorkflowyDataConverter.storeHighWatermark(backupInstant);

				return null;
			});

		LOGGER.info("Completed merge for backup file: {}", this.backupFile.getName());
	}

	private static Instant getHighWatermark() {
		Operation workflowyCriteria = DataImportTimestampFinder.name().eq("workflowy");
		DataImportTimestamp workflowyTimestamp = DataImportTimestampFinder.findOne(workflowyCriteria);

		Instant highWatermark = Optional.ofNullable(workflowyTimestamp)
			.map(DataImportTimestamp::getTimestamp)
			.map(Timestamp::toInstant)
			.orElse(Instant.MIN);

		LOGGER.info("High watermark: {}", highWatermark);
		return highWatermark;
	}

	private static void storeHighWatermark(@Nonnull Instant instant) {
		Timestamp highWatermark = Timestamp.from(instant);
		Operation workflowyCriteria = DataImportTimestampFinder.name().eq("workflowy");
		DataImportTimestamp workflowyTimestamp = DataImportTimestampFinder.findOne(workflowyCriteria);

		if (workflowyTimestamp == null) {
			DataImportTimestamp newTimestamp = new DataImportTimestamp();
			newTimestamp.setName("workflowy");
			newTimestamp.setTimestamp(highWatermark);
			newTimestamp.insert();
		} else {
			workflowyTimestamp.setTimestamp(highWatermark);
		}

		LOGGER.info("Stored high watermark: {}", instant);
	}

	private static ImmutableList<File> getBackupFiles(Path backupsPath) {
		File[] files = backupsPath.toFile().listFiles((pathname) -> pathname.getName().endsWith(".workflowy.backup"));
		Objects.requireNonNull(files, backupsPath::toString);
		return ArrayAdapter.adapt(files).toSortedListBy(File::getName).toImmutable();
	}

	private static boolean isAfterHighWatermark(File file, Instant highWatermark) {
		Instant fileTimestamp = getFileTimestamp(file);
		return fileTimestamp.isAfter(highWatermark);
	}

	private static Instant getFileTimestamp(File file) {
		String fileName = file.getName();
		Matcher matcher = FILE_DATE_PATTERN.matcher(fileName);
		if (matcher.find()) {
			LocalDate date = LocalDate.parse(matcher.group(1), DateTimeFormatter.ISO_LOCAL_DATE);
			return date.atStartOfDay().toInstant(ZoneOffset.UTC);
		}
		return Instant.MIN;
	}

	private static Timestamp convertWorkflowyTimestamp(Long workflowyTimestamp) {
		if (workflowyTimestamp == null) {
			return null;
		}
		long epochSeconds = workflowyTimestamp + WORKFLOWY_EPOCH_OFFSET;
		return Timestamp.from(Instant.ofEpochSecond(epochSeconds));
	}

	private static Timestamp parseCalendarDate(Object dateValue) {
		if (dateValue instanceof Number number) {
			long epochSeconds = number.longValue();
			return Timestamp.from(Instant.ofEpochSecond(epochSeconds));
		}
		return null;
	}
}
