package com.workflowy.data.converter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gs.fw.common.mithra.MithraManagerProvider;
import com.gs.fw.common.mithra.finder.Operation;
import com.gs.fw.common.mithra.list.merge.TopLevelMergeOptions;
import com.workflowy.ApiImportTimestamp;
import com.workflowy.ApiImportTimestampFinder;
import com.workflowy.Backlink;
import com.workflowy.BacklinkFinder;
import com.workflowy.BacklinkList;
import com.workflowy.BackupImportTimestamp;
import com.workflowy.BackupImportTimestampFinder;
import com.workflowy.Mirror;
import com.workflowy.MirrorFinder;
import com.workflowy.MirrorList;
import com.workflowy.NodeCalendar;
import com.workflowy.NodeCalendarFinder;
import com.workflowy.NodeCalendarLevels;
import com.workflowy.NodeCalendarList;
import com.workflowy.NodeContent;
import com.workflowy.NodeContentFinder;
import com.workflowy.NodeContentList;
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
import com.workflowy.data.pojo.*;
import cool.klass.data.store.DataStore;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.eclipse.collections.impl.utility.MapIterate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorkflowyDataConverter {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowyDataConverter.class);

	private final ObjectMapper objectMapper;
	private final DataStore dataStore;
	private final File backupFile;
	private final String userId;

	private final MutableMap<String, NodeContent> nodeContents = MapAdapter.adapt(new LinkedHashMap<>());
	private final MutableMap<String, NodeMetadata> nodeMetadatas = MapAdapter.adapt(new LinkedHashMap<>());
	private final MutableMap<String, Tag> tags = MapAdapter.adapt(new LinkedHashMap<>());
	private final NodeTagMappingList nodeTagMappings = new NodeTagMappingList();
	private final MirrorList mirrors = new MirrorList();
	private final BacklinkList backlinks = new BacklinkList();
	private final NodeCalendarList nodeCalendars = new NodeCalendarList();
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
		this.userId = WorkflowyFileUtils.extractUserIdFromFile(backupFile);
	}

	public static void convert(
		@Nonnull Path backupsPath,
		@Nonnull ObjectMapper objectMapper,
		@Nonnull DataStore dataStore
	) {
		Instant highWatermark = WorkflowyDataConverter.getBackupHighWatermark();

		ImmutableList<File> filesToProcess = WorkflowyDataConverter.getBackupFiles(backupsPath).selectWith(
			WorkflowyFileUtils::isAfterHighWatermark,
			highWatermark
		);

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

		Instant backupInstant = WorkflowyFileUtils.getFileTimestamp(this.backupFile);

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

		LOGGER.info("Pass 3: Processing metadata (mirrors, backlinks, dates, S3 files, virtual roots)");
		this.processMetadata(rootItems);
		LOGGER.info(
			"Created {} mirrors, {} backlinks, {} node dates, {} S3 files, {} virtual root mappings",
			this.mirrors.size(),
			this.backlinks.size(),
			this.nodeCalendars.size(),
			this.nodeS3Files.size(),
			this.virtualRootMappings.size()
		);

		this.mergeIntoDatabase(backupInstant);
	}

	private void processNodesPass1(List<InputItem> inputItems, String parentId, int startPriority) {
		// Use multiples of 100 for priority to match API format (0, 100, 200, ...)
		int priority = startPriority;
		for (InputItem inputItem : inputItems) {
			NodeContent nodeContent = this.createNodeContent(inputItem, parentId);
			NodeMetadata nodeMetadata = this.createNodeMetadata(inputItem, priority);
			this.nodeContents.put(inputItem.id(), nodeContent);
			this.nodeMetadatas.put(inputItem.id(), nodeMetadata);
			priority += 100;

			if (inputItem.hasChildren()) {
				this.processNodesPass1(inputItem.children(), inputItem.id(), 0);
			}
		}
	}

	private NodeContent createNodeContent(InputItem inputItem, String parentId) {
		var nodeContent = new NodeContent();
		nodeContent.setId(inputItem.id());
		nodeContent.setParentId(parentId);
		nodeContent.setName(inputItem.name() != null ? inputItem.name() : "");
		nodeContent.setNote(inputItem.note());
		return nodeContent;
	}

	private NodeMetadata createNodeMetadata(InputItem inputItem, int priority) {
		var nodeMetadata = new NodeMetadata();
		nodeMetadata.setNodeId(inputItem.id());
		nodeMetadata.setShortId(WorkflowyFileUtils.computeShortId(inputItem.id()));
		nodeMetadata.setPriority(priority);
		nodeMetadata.setCompletedAt(
			WorkflowyTimestampConverter.convertWorkflowyTimestamp(inputItem.completedTimestamp())
		);
		nodeMetadata.setLastModified(
			WorkflowyTimestampConverter.convertWorkflowyTimestamp(inputItem.lastModifiedTimestamp())
		);
		nodeMetadata.setCreatedById(this.userId);
		nodeMetadata.setCreatedOn(WorkflowyTimestampConverter.convertWorkflowyTimestamp(inputItem.createdTimestamp()));
		nodeMetadata.setLastUpdatedById(this.userId);

		InputMetadata metadata = inputItem.metadata();
		if (metadata != null) {
			nodeMetadata.setLayoutMode(normalizeLayoutMode(metadata.layoutMode()));

			// Set inChat consistently with API converter (false when not present)
			InputAiMetadata ai = metadata.ai();
			nodeMetadata.setInChat(ai != null && Boolean.TRUE.equals(ai.inChat()));

			if (metadata.changes() != null) {
				try {
					nodeMetadata.setChanges(this.objectMapper.writeValueAsString(metadata.changes()));
				} catch (Exception e) {
					LOGGER.warn("Failed to serialize changes for node {}: {}", inputItem.id(), e.getMessage());
				}
			}

			if (metadata.numberedStart() != null) {
				nodeMetadata.setNumberedStart(metadata.numberedStart());
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
					var newTag = new Tag();
					newTag.setName(t);
					newTag.setColor(null);
					return newTag;
				});

			var mapping = new NodeTagMapping();
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
		// Variant 1: { originalId: "...", isMirrorRoot: true } — this node is a mirror of originalId
		if (mirrorMeta.originalId() != null) {
			var mirror = new Mirror();
			mirror.setOriginalId(mirrorMeta.originalId());
			mirror.setMirrorId(nodeId);
			this.mirrors.add(mirror);
		}

		// Variant 2: { mirrorRootIds: { "id1": true, ... } } — each key is an original this node mirrors
		for (String sourceId : mirrorMeta.getMirrorSourceIds()) {
			var mirror = new Mirror();
			mirror.setOriginalId(sourceId);
			mirror.setMirrorId(nodeId);
			this.mirrors.add(mirror);
		}

		// Variant 3: { backlinkMirrorRootIds: { "id1": true, ... } } — reversed: this node is the original
		for (String backlinkId : mirrorMeta.getBacklinkMirrorIds()) {
			var mirror = new Mirror();
			mirror.setOriginalId(nodeId);
			mirror.setMirrorId(backlinkId);
			this.mirrors.add(mirror);
		}
	}

	private MirrorList deduplicateMirrors() {
		Set<String> seen = new LinkedHashSet<>();
		var result = new MirrorList();
		for (Mirror mirror : this.mirrors) {
			String key = mirror.getOriginalId() + "|" + mirror.getMirrorId();
			if (seen.add(key)) {
				result.add(mirror);
			}
		}
		return result;
	}

	private void processBacklinkMetadata(InputBacklinkMetadata backlinkMeta) {
		if (backlinkMeta.sourceId() != null && backlinkMeta.targetId() != null) {
			var backlink = new Backlink();
			backlink.setSourceId(backlinkMeta.sourceId());
			backlink.setTargetId(backlinkMeta.targetId());
			this.backlinks.add(backlink);
		}
	}

	private void processCalendarMetadata(String nodeId, InputCalendarMetadata calendarMeta) {
		if (calendarMeta.date() != null) {
			Timestamp dateValue = WorkflowyTimestampConverter.parseCalendarDate(calendarMeta.date());
			if (dateValue != null) {
				var nodeCalendar = new NodeCalendar();
				nodeCalendar.setId(UUID.randomUUID().toString());
				nodeCalendar.setNodeId(nodeId);
				nodeCalendar.setDateValue(dateValue);
				nodeCalendar.setRoot(calendarMeta.isRoot());
				nodeCalendar.setLevel(calendarMeta.level());
				nodeCalendar.setDateId(calendarMeta.dateId());
				nodeCalendar.setTimestamp(calendarMeta.timestamp());
				nodeCalendar.setValue(calendarMeta.value());
				if (calendarMeta.levels() != null) {
					var levels = new NodeCalendarLevels();
					levels.setCalendarId(nodeCalendar.getId());
					levels.setDay(calendarMeta.levels().day());
					levels.setWeek(calendarMeta.levels().week());
					levels.setMonth(calendarMeta.levels().month());
					levels.setYear(calendarMeta.levels().year());
					nodeCalendar.setLevels(levels);
				}
				if (calendarMeta.foundDates() != null) {
					try {
						nodeCalendar.setFoundDates(this.objectMapper.writeValueAsString(calendarMeta.foundDates()));
					} catch (Exception e) {
						LOGGER.warn("Failed to serialize foundDates for node {}: {}", nodeId, e.getMessage());
					}
				}
				this.nodeCalendars.add(nodeCalendar);
			}
		}
	}

	private void processS3FileMetadata(String nodeId, InputS3FileMetadata s3FileMeta) {
		var nodeS3File = new NodeS3File();
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
			var mapping = new VirtualRootMapping();
			mapping.setNodeId(nodeId);
			mapping.setVirtualRootId(virtualRootId);
			this.virtualRootMappings.add(mapping);
		}
	}

	private void ensureUserExists() {
		User existingUser = UserFinder.findOne(UserFinder.userId().eq(this.userId));
		if (existingUser == null) {
			LOGGER.info("Creating user: {}", this.userId);
			MithraManagerProvider.getMithraManager().executeTransactionalCommand((tx) -> {
					var user = new User();
					user.setUserId(this.userId);
					user.setEmail(this.userId);
					user.insert();
					return null;
				});
		}
	}

	private void mergeIntoDatabase(Instant backupFileDate) {
		// Use actual wall-clock time for SYSTEM_TIME, not the backfilled backup date
		Instant importTime = Instant.now();

		// Validate that this import won't violate temporal ordering
		validateImportTime(importTime, backupFileDate);

		this.ensureUserExists();

		MithraManagerProvider.getMithraManager().setTransactionTimeout(3600);

		long time = importTime.toEpochMilli();

		this.dataStore.runInTransaction((transaction) -> {
				transaction.setSystemTime(time);

				LOGGER.info("Merging {} tags", this.tags.size());
				TagList existingTags = TagFinder.findMany(TagFinder.all());
				var updatedTags = new TagList();
				updatedTags.addAll(this.tags.values());
				var tagMergeOptions = new TopLevelMergeOptions<Tag>(TagFinder.getFinderInstance());
				tagMergeOptions.doNotCompare(TagFinder.systemFrom(), TagFinder.systemTo());
				existingTags.merge(updatedTags, tagMergeOptions);

				LOGGER.info("Merging {} node contents", this.nodeContents.size());
				NodeContentList existingContents = NodeContentFinder.findMany(NodeContentFinder.all());
				var updatedContents = new NodeContentList();
				updatedContents.addAll(this.nodeContents.values());
				var contentMergeOptions = new TopLevelMergeOptions<NodeContent>(NodeContentFinder.getFinderInstance());
				contentMergeOptions.doNotCompare(NodeContentFinder.systemFrom(), NodeContentFinder.systemTo());
				existingContents.merge(updatedContents, contentMergeOptions);

				LOGGER.info("Merging {} node metadatas", this.nodeMetadatas.size());
				NodeMetadataList existingMetadatas = NodeMetadataFinder.findMany(NodeMetadataFinder.all());

				// Apply order detection to minimize priority-related updates.
				// This preserves existing priorities when sibling order hasn't changed.
				Map<String, Integer> priorityUpdates = this.calculatePrioritiesWithOrderDetection(existingMetadatas);
				LOGGER.info(
					"Order detection: {} nodes need priority updates out of {}",
					priorityUpdates.size(),
					this.nodeMetadatas.size()
				);

				// Build map of existing priorities for preservation
				MutableMap<String, Integer> existingPriorities = Maps.mutable.empty();
				for (NodeMetadata meta : existingMetadatas) {
					existingPriorities.put(meta.getNodeId(), meta.getPriority());
				}

				// Apply priorities: use calculated updates, or preserve existing
				for (NodeMetadata meta : this.nodeMetadatas.values()) {
					String nodeId = meta.getNodeId();
					if (priorityUpdates.containsKey(nodeId)) {
						// Order changed or new node - use calculated priority
						meta.setPriority(priorityUpdates.get(nodeId));
					} else if (existingPriorities.containsKey(nodeId)) {
						// Order unchanged - preserve existing priority
						meta.setPriority(existingPriorities.get(nodeId));
					}
					// New nodes not in priorityUpdates keep their initial priority (shouldn't happen)
				}

				var updatedMetadatas = new NodeMetadataList();
				updatedMetadatas.addAll(this.nodeMetadatas.values());
				var metadataMergeOptions = new TopLevelMergeOptions<NodeMetadata>(
					NodeMetadataFinder.getFinderInstance()
				);
				// Exclude temporal and audit fields
				metadataMergeOptions.doNotCompare(
					NodeMetadataFinder.systemFrom(),
					NodeMetadataFinder.systemTo(),
					NodeMetadataFinder.createdById(),
					NodeMetadataFinder.createdOn(),
					NodeMetadataFinder.lastUpdatedById()
				);
				// Exclude priority - backup derives it from tree position, not real priority.
				// API import has the real priorities and should update them.
				metadataMergeOptions.doNotCompare(NodeMetadataFinder.priority());
				// Exclude fields not compared in TypeScript backup import.
				// Only compare: completed, completedAt, layoutMode
				metadataMergeOptions.doNotCompare(
					NodeMetadataFinder.inChat(),
					NodeMetadataFinder.changes(),
					NodeMetadataFinder.numberedStart(),
					NodeMetadataFinder.lastModified() // Exclude timestamp-only changes
				);
				existingMetadatas.merge(updatedMetadatas, metadataMergeOptions);

				LOGGER.info("Merging {} node-tag mappings", this.nodeTagMappings.size());
				NodeTagMappingList existingMappings = NodeTagMappingFinder.findMany(NodeTagMappingFinder.all());
				var mappingMergeOptions = new TopLevelMergeOptions<NodeTagMapping>(
					NodeTagMappingFinder.getFinderInstance()
				);
				mappingMergeOptions.doNotCompare(NodeTagMappingFinder.systemFrom(), NodeTagMappingFinder.systemTo());
				existingMappings.merge(this.nodeTagMappings, mappingMergeOptions);

				MirrorList deduplicatedMirrors = this.deduplicateMirrors();
				LOGGER.info("Merging {} mirrors ({} before dedup)", deduplicatedMirrors.size(), this.mirrors.size());
				MirrorList existingMirrors = MirrorFinder.findMany(MirrorFinder.all());
				var mirrorMergeOptions = new TopLevelMergeOptions<Mirror>(MirrorFinder.getFinderInstance());
				mirrorMergeOptions.doNotCompare(MirrorFinder.systemFrom(), MirrorFinder.systemTo());
				existingMirrors.merge(deduplicatedMirrors, mirrorMergeOptions);

				LOGGER.info("Merging {} backlinks", this.backlinks.size());
				BacklinkList existingBacklinks = BacklinkFinder.findMany(BacklinkFinder.all());
				var backlinkMergeOptions = new TopLevelMergeOptions<Backlink>(BacklinkFinder.getFinderInstance());
				backlinkMergeOptions.doNotCompare(BacklinkFinder.systemFrom(), BacklinkFinder.systemTo());
				existingBacklinks.merge(this.backlinks, backlinkMergeOptions);

				LOGGER.info("Merging {} node calendars", this.nodeCalendars.size());
				NodeCalendarList existingCalendars = NodeCalendarFinder.findMany(NodeCalendarFinder.all());
				var calendarMergeOptions = new TopLevelMergeOptions<NodeCalendar>(
					NodeCalendarFinder.getFinderInstance()
				);
				calendarMergeOptions.doNotCompare(NodeCalendarFinder.systemFrom(), NodeCalendarFinder.systemTo());
				existingCalendars.merge(this.nodeCalendars, calendarMergeOptions);

				LOGGER.info("Merging {} node S3 files", this.nodeS3Files.size());
				NodeS3FileList existingS3Files = NodeS3FileFinder.findMany(NodeS3FileFinder.all());
				var s3FileMergeOptions = new TopLevelMergeOptions<NodeS3File>(NodeS3FileFinder.getFinderInstance());
				s3FileMergeOptions.doNotCompare(NodeS3FileFinder.systemFrom(), NodeS3FileFinder.systemTo());
				existingS3Files.merge(this.nodeS3Files, s3FileMergeOptions);

				LOGGER.info("Merging {} virtual root mappings", this.virtualRootMappings.size());
				VirtualRootMappingList existingVirtualRoots = VirtualRootMappingFinder.findMany(
					VirtualRootMappingFinder.all()
				);
				var virtualRootMergeOptions = new TopLevelMergeOptions<VirtualRootMapping>(
					VirtualRootMappingFinder.getFinderInstance()
				);
				virtualRootMergeOptions.doNotCompare(
					VirtualRootMappingFinder.systemFrom(),
					VirtualRootMappingFinder.systemTo()
				);
				existingVirtualRoots.merge(this.virtualRootMappings, virtualRootMergeOptions);

				WorkflowyDataConverter.storeBackupHighWatermark(backupFileDate);

				return null;
			});

		LOGGER.info("Completed merge for backup file: {}", this.backupFile.getName());
	}

	/**
	 * Holds information about a sibling node for order detection.
	 * backupIndex is the position in the backup file (0, 1, 2, ...).
	 */
	private record SiblingInfo(String nodeId, int backupIndex, Integer existingPriority) {
		boolean isNew() {
			return this.existingPriority == null;
		}
	}

	/**
	 * Calculates priorities using order detection to minimize unnecessary updates.
	 * Only returns new priorities for nodes that actually need priority changes.
	 *
	 * <p>The algorithm detects whether the ORDER of siblings has changed, not just
	 * their priority values. If siblings A, B, C exist in the DB and the backup
	 * shows them in the same order, their existing priorities are preserved.
	 */
	private Map<String, Integer> calculatePrioritiesWithOrderDetection(NodeMetadataList existingMetadatas) {
		// Build map of existing priorities
		MutableMap<String, Integer> existingPriorities = Maps.mutable.empty();
		for (NodeMetadata meta : existingMetadatas) {
			existingPriorities.put(meta.getNodeId(), meta.getPriority());
		}

		// Group nodes by parent using nodeContents (which has parentId)
		Map<String, List<SiblingInfo>> siblingsByParent = new LinkedHashMap<>();
		int backupIndex = 0;
		for (var entry : this.nodeContents.entrySet()) {
			String nodeId = entry.getKey();
			NodeContent content = entry.getValue();
			String parentId = content.getParentId();
			// Use empty string for root nodes
			String groupKey = parentId != null ? parentId : "";

			siblingsByParent
				.computeIfAbsent(groupKey, (k) -> Lists.mutable.empty())
				.add(new SiblingInfo(nodeId, siblingsByParent.get(groupKey).size(), existingPriorities.get(nodeId)));
		}

		// For each sibling group, detect if order changed
		Map<String, Integer> priorityUpdates = new LinkedHashMap<>();
		for (Map.Entry<String, List<SiblingInfo>> group : siblingsByParent.entrySet()) {
			List<SiblingInfo> siblings = group.getValue();

			// Check if any siblings are new
			boolean hasNewNodes = siblings.stream().anyMatch(SiblingInfo::isNew);

			// Get existing siblings sorted by their existing priority
			List<SiblingInfo> existingSiblings = siblings
				.stream()
				.filter((s) -> !s.isNew())
				.sorted(Comparator.comparingInt(SiblingInfo::existingPriority))
				.toList();

			// Compare order: extract node IDs in backup order vs DB order
			List<String> backupOrder = siblings
				.stream()
				.filter((s) -> !s.isNew())
				.map(SiblingInfo::nodeId)
				.toList();
			List<String> dbOrder = existingSiblings.stream().map(SiblingInfo::nodeId).toList();

			boolean orderChanged = !backupOrder.equals(dbOrder);

			if (orderChanged || hasNewNodes) {
				// Recalculate all priorities for this group
				int priority = 0;
				for (SiblingInfo sibling : siblings) {
					priorityUpdates.put(sibling.nodeId(), priority);
					priority += 100;
				}
			}
			// If order unchanged and no new nodes, don't add to priorityUpdates
			// (existing priorities will be preserved by the caller)
		}

		return priorityUpdates;
	}

	private static Instant getBackupHighWatermark() {
		Operation workflowyCriteria = BackupImportTimestampFinder.name().eq("workflowy");
		BackupImportTimestamp workflowyTimestamp = BackupImportTimestampFinder.findOne(workflowyCriteria);

		Instant highWatermark = Optional.ofNullable(workflowyTimestamp)
			.map(BackupImportTimestamp::getTimestamp)
			.map(Timestamp::toInstant)
			.orElse(Instant.MIN);

		LOGGER.info("Backup high watermark: {}", highWatermark);
		return highWatermark;
	}

	private static Instant getApiHighWatermark() {
		Operation criteria = ApiImportTimestampFinder.name().eq("workflowy");
		ApiImportTimestamp timestamp = ApiImportTimestampFinder.findOne(criteria);
		return Optional.ofNullable(timestamp)
			.map(ApiImportTimestamp::getTimestamp)
			.map(Timestamp::toInstant)
			.orElse(Instant.MIN);
	}

	private static void validateImportTime(Instant proposedTime, Instant backupFileDate) {
		Instant maxBackupTime = getBackupHighWatermark();
		Instant maxApiTime = getApiHighWatermark();
		Instant maxTime = maxBackupTime.isAfter(maxApiTime) ? maxBackupTime : maxApiTime;

		if (proposedTime.isBefore(maxTime)) {
			throw new IllegalStateException(
				"Cannot import backup with date "
				+ backupFileDate
				+ " which would create SYSTEM_FROM "
				+ proposedTime
				+ " before existing data at "
				+ maxTime
				+ ". Use 'rollback-temporal' command first to roll back to before this date."
			);
		}
	}

	private static void storeBackupHighWatermark(@Nonnull Instant instant) {
		Timestamp highWatermark = Timestamp.from(instant);
		Operation workflowyCriteria = BackupImportTimestampFinder.name().eq("workflowy");
		BackupImportTimestamp workflowyTimestamp = BackupImportTimestampFinder.findOne(workflowyCriteria);

		if (workflowyTimestamp == null) {
			var newTimestamp = new BackupImportTimestamp();
			newTimestamp.setName("workflowy");
			newTimestamp.setTimestamp(highWatermark);
			newTimestamp.insert();
		} else {
			workflowyTimestamp.setTimestamp(highWatermark);
		}

		LOGGER.info("Stored backup high watermark: {}", instant);
	}

	/**
	 * Resets the import watermark to allow re-processing all backup files.
	 * Use this to recover from temporal corruption or to force a full re-import.
	 */
	public static void resetWatermark(@Nonnull DataStore dataStore) {
		dataStore.runInTransaction((transaction) -> {
			Operation workflowyCriteria = BackupImportTimestampFinder.name().eq("workflowy");
			BackupImportTimestamp workflowyTimestamp = BackupImportTimestampFinder.findOne(workflowyCriteria);

			if (workflowyTimestamp != null) {
				LOGGER.info("Deleting backup watermark with timestamp: {}", workflowyTimestamp.getTimestamp());
				workflowyTimestamp.delete();
			} else {
				LOGGER.info("No backup watermark found to reset");
			}
			return null;
		});
	}

	private static ImmutableList<File> getBackupFiles(Path backupsPath) {
		File directory = backupsPath.toFile();
		if (!directory.exists()) {
			throw new IllegalArgumentException("Backup directory does not exist: " + backupsPath);
		}
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("Backup path is not a directory: " + backupsPath);
		}
		File[] files = directory.listFiles((pathname) -> pathname.getName().endsWith(".workflowy.backup"));
		Objects.requireNonNull(files, backupsPath::toString);
		return ArrayAdapter.adapt(files).toSortedListBy(File::getName).toImmutable();
	}

	/**
	 * Normalizes layoutMode to prevent edit wars between backup and API imports.
	 * Backup files store the default layout as "bullets", while API exports use null.
	 * We normalize "bullets" to null so both sources produce consistent values.
	 */
	private static String normalizeLayoutMode(String layoutMode) {
		if ("bullets".equals(layoutMode)) {
			return null;
		}
		return layoutMode;
	}
}
