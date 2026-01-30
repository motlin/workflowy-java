package com.workflowy.data.converter;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gs.fw.common.mithra.MithraManagerProvider;
import com.gs.fw.common.mithra.finder.Operation;
import com.gs.fw.common.mithra.list.merge.TopLevelMergeOptions;
import com.workflowy.ApiImportTimestamp;
import com.workflowy.ApiImportTimestampFinder;
import com.workflowy.BackupImportTimestamp;
import com.workflowy.BackupImportTimestampFinder;
import com.workflowy.NodeContent;
import com.workflowy.NodeContentFinder;
import com.workflowy.NodeContentList;
import com.workflowy.NodeMetadata;
import com.workflowy.NodeMetadataFinder;
import com.workflowy.NodeMetadataList;
import com.workflowy.NodeTagMapping;
import com.workflowy.NodeTagMappingFinder;
import com.workflowy.NodeTagMappingList;
import com.workflowy.Tag;
import com.workflowy.TagFinder;
import com.workflowy.TagList;
import com.workflowy.User;
import com.workflowy.UserFinder;
import com.workflowy.data.pojo.ApiInputItem;
import com.workflowy.data.pojo.ApiResponse;
import com.workflowy.data.pojo.InputAiMetadata;
import cool.klass.data.store.DataStore;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converter for Workflowy API export data.
 *
 * <p>This converter handles the flat list format returned by the API export endpoint
 * (GET https://workflowy.com/api/v1/nodes-export), which differs from the nested
 * backup format in several ways:
 * <ul>
 *   <li>Flat structure with explicit parent_id (no nested children)</li>
 *   <li>Uses standard Unix timestamps (seconds since 1970)</li>
 *   <li>Simpler metadata structure (no mirrors, calendar, s3File)</li>
 * </ul>
 *
 * @see WorkflowyDataConverter for the backup format converter
 */
public final class WorkflowyApiDataConverter {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowyApiDataConverter.class);

	private static final String DEFAULT_USER_ID = "api-import";

	private final ObjectMapper objectMapper;
	private final DataStore dataStore;
	private final String userId;

	private final MutableMap<String, NodeContent> nodeContents = MapAdapter.adapt(new LinkedHashMap<>());
	private final MutableMap<String, NodeMetadata> nodeMetadatas = MapAdapter.adapt(new LinkedHashMap<>());
	private final MutableMap<String, Tag> tags = MapAdapter.adapt(new LinkedHashMap<>());
	private final NodeTagMappingList nodeTagMappings = new NodeTagMappingList();

	private WorkflowyApiDataConverter(
		@Nonnull ObjectMapper objectMapper,
		@Nonnull DataStore dataStore,
		@Nonnull String userId
	) {
		this.objectMapper = Objects.requireNonNull(objectMapper);
		this.dataStore = Objects.requireNonNull(dataStore);
		this.userId = Objects.requireNonNull(userId);
	}

	/**
	 * Convert nodes from an API export JSON file into the database.
	 *
	 * @param apiExportFile   Path to the API export JSON file
	 * @param objectMapper    Jackson ObjectMapper for JSON parsing
	 * @param dataStore       Data store for database operations
	 */
	public static void convertFromFile(
		@Nonnull File apiExportFile,
		@Nonnull ObjectMapper objectMapper,
		@Nonnull DataStore dataStore
	) {
		LOGGER.info("Processing API export file: {}", apiExportFile);
		WorkflowyApiDataConverter converter = new WorkflowyApiDataConverter(objectMapper, dataStore, DEFAULT_USER_ID);

		try {
			ApiResponse apiResponse = objectMapper.readValue(apiExportFile, ApiResponse.class);
			List<ApiInputItem> nodes = apiResponse.nodes();
			converter.processNodes(nodes, Instant.now());
		} catch (IOException e) {
			throw new RuntimeException("Failed to process API export file: " + apiExportFile, e);
		}
	}

	/**
	 * Convert nodes from an API export directly (no file).
	 *
	 * @param nodes        List of API nodes to import
	 * @param objectMapper Jackson ObjectMapper
	 * @param dataStore    Data store for database operations
	 * @param importTime   Timestamp to use for the import
	 */
	public static void convertFromNodes(
		@Nonnull List<ApiInputItem> nodes,
		@Nonnull ObjectMapper objectMapper,
		@Nonnull DataStore dataStore,
		@Nonnull Instant importTime
	) {
		LOGGER.info("Processing {} API export nodes", nodes.size());
		WorkflowyApiDataConverter converter = new WorkflowyApiDataConverter(objectMapper, dataStore, DEFAULT_USER_ID);
		converter.processNodes(nodes, importTime);
	}

	private void processNodes(List<ApiInputItem> nodes, Instant importTime) {
		LOGGER.info("Pass 1: Creating node content and metadata from {} nodes", nodes.size());
		for (ApiInputItem node : nodes) {
			NodeContent nodeContent = this.createNodeContent(node);
			NodeMetadata nodeMetadata = this.createNodeMetadata(node);
			this.nodeContents.put(node.id(), nodeContent);
			this.nodeMetadatas.put(node.id(), nodeMetadata);
		}
		LOGGER.info(
			"Created {} node contents and {} node metadatas",
			this.nodeContents.size(),
			this.nodeMetadatas.size()
		);

		LOGGER.info("Pass 2: Extracting tags");
		this.extractTagsFromNodes();
		LOGGER.info("Extracted {} tags and {} node-tag mappings", this.tags.size(), this.nodeTagMappings.size());

		this.mergeIntoDatabase(importTime);
	}

	private NodeContent createNodeContent(ApiInputItem node) {
		NodeContent nodeContent = new NodeContent();
		nodeContent.setId(node.id());
		nodeContent.setParentId(node.parentId());
		nodeContent.setName(node.name() != null ? node.name() : "");
		nodeContent.setNote(emptyToNull(node.note()));
		return nodeContent;
	}

	private static String emptyToNull(String value) {
		return value == null || value.isEmpty() ? null : value;
	}

	private NodeMetadata createNodeMetadata(ApiInputItem node) {
		NodeMetadata nodeMetadata = new NodeMetadata();
		nodeMetadata.setNodeId(node.id());
		nodeMetadata.setPriority(node.priority());
		nodeMetadata.setCompletedAt(WorkflowyTimestampConverter.convertUnixTimestamp(node.completedAt()));
		nodeMetadata.setCollapsed(false);
		nodeMetadata.setLastModified(WorkflowyTimestampConverter.convertUnixTimestamp(node.modifiedAt()));
		nodeMetadata.setCreatedById(this.userId);
		nodeMetadata.setCreatedOn(WorkflowyTimestampConverter.convertUnixTimestamp(node.createdAt()));
		nodeMetadata.setLastUpdatedById(this.userId);

		nodeMetadata.setLayoutMode(normalizeLayoutMode(node.data().layoutMode()));
		nodeMetadata.setReferencesRoot(Boolean.TRUE.equals(node.data().isReferencesRoot()));
		InputAiMetadata ai = node.data().ai();
		nodeMetadata.setInChat(ai != null && Boolean.TRUE.equals(ai.inChat()));

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

	private void ensureUserExists() {
		User existingUser = UserFinder.findOne(UserFinder.userId().eq(this.userId));
		if (existingUser == null) {
			LOGGER.info("Creating user: {}", this.userId);
			MithraManagerProvider.getMithraManager().executeTransactionalCommand((tx) -> {
					User user = new User();
					user.setUserId(this.userId);
					user.setEmail(this.userId);
					user.insert();
					return null;
				});
		}
	}

	private void mergeIntoDatabase(Instant importTime) {
		// Validate that this import won't violate temporal ordering
		validateImportTime(importTime);

		this.ensureUserExists();

		MithraManagerProvider.getMithraManager().setTransactionTimeout(3600);

		long time = importTime.toEpochMilli();

		this.dataStore.runInTransaction((transaction) -> {
				transaction.setSystemTime(time);

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
				// Exclude audit fields
				metadataMergeOptions.doNotCompare(
					NodeMetadataFinder.createdById(),
					NodeMetadataFinder.createdOn(),
					NodeMetadataFinder.lastUpdatedById()
				);
				// Exclude fields not provided or incomplete in the API export format
				metadataMergeOptions.doNotCompare(
					NodeMetadataFinder.virtualRoot(),
					NodeMetadataFinder.mirrorRoot(),
					NodeMetadataFinder.originalId(),
					NodeMetadataFinder.changes(),
					NodeMetadataFinder.numberedStart(),
					NodeMetadataFinder.collapsed(),
					NodeMetadataFinder.lastModified() // API often returns null, preserve backup values
				);
				existingMetadatas.merge(updatedMetadatas, metadataMergeOptions);

				LOGGER.info("Merging {} node-tag mappings", this.nodeTagMappings.size());
				NodeTagMappingList existingMappings = NodeTagMappingFinder.findMany(NodeTagMappingFinder.all());
				TopLevelMergeOptions<NodeTagMapping> mappingMergeOptions = new TopLevelMergeOptions<>(
					NodeTagMappingFinder.getFinderInstance()
				);
				existingMappings.merge(this.nodeTagMappings, mappingMergeOptions);

				storeApiHighWatermark(importTime);

				return null;
			});

		LOGGER.info("Completed API export import");
	}

	private static Instant getBackupHighWatermark() {
		Operation criteria = BackupImportTimestampFinder.name().eq("workflowy");
		BackupImportTimestamp timestamp = BackupImportTimestampFinder.findOne(criteria);
		return Optional.ofNullable(timestamp)
			.map(BackupImportTimestamp::getTimestamp)
			.map(Timestamp::toInstant)
			.orElse(Instant.MIN);
	}

	private static Instant getApiHighWatermark() {
		Operation criteria = ApiImportTimestampFinder.name().eq("workflowy");
		ApiImportTimestamp timestamp = ApiImportTimestampFinder.findOne(criteria);
		return Optional.ofNullable(timestamp)
			.map(ApiImportTimestamp::getTimestamp)
			.map(Timestamp::toInstant)
			.orElse(Instant.MIN);
	}

	private static void validateImportTime(Instant proposedTime) {
		Instant maxBackupTime = getBackupHighWatermark();
		Instant maxApiTime = getApiHighWatermark();
		Instant maxTime = maxBackupTime.isAfter(maxApiTime) ? maxBackupTime : maxApiTime;

		if (proposedTime.isBefore(maxTime)) {
			throw new IllegalStateException(
				"Cannot import API data with time "
				+ proposedTime
				+ " which is before existing data at "
				+ maxTime
				+ ". Use 'rollback-temporal' command first to roll back to before this date."
			);
		}
	}

	private static void storeApiHighWatermark(@Nonnull Instant instant) {
		Timestamp watermark = Timestamp.from(instant);
		Operation criteria = ApiImportTimestampFinder.name().eq("workflowy");
		ApiImportTimestamp timestamp = ApiImportTimestampFinder.findOne(criteria);

		if (timestamp == null) {
			ApiImportTimestamp newTimestamp = new ApiImportTimestamp();
			newTimestamp.setName("workflowy");
			newTimestamp.setTimestamp(watermark);
			newTimestamp.insert();
		} else {
			timestamp.setTimestamp(watermark);
		}

		LOGGER.info("Stored API high watermark: {}", instant);
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
