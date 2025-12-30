package com.workflowy.data.converter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gs.fw.common.mithra.finder.Operation;
import com.gs.fw.common.mithra.list.merge.TopLevelMergeOptions;
import com.workflowy.DataImportTimestamp;
import com.workflowy.DataImportTimestampFinder;
import com.workflowy.Item;
import com.workflowy.ItemDate;
import com.workflowy.ItemDateFinder;
import com.workflowy.ItemDateList;
import com.workflowy.ItemFinder;
import com.workflowy.ItemList;
import com.workflowy.ItemTagMapping;
import com.workflowy.ItemTagMappingFinder;
import com.workflowy.ItemTagMappingList;
import com.workflowy.Mirror;
import com.workflowy.MirrorFinder;
import com.workflowy.MirrorList;
import com.workflowy.Tag;
import com.workflowy.TagFinder;
import com.workflowy.TagList;
import com.workflowy.data.pojo.InputBacklinkMetadata;
import com.workflowy.data.pojo.InputCalendarMetadata;
import com.workflowy.data.pojo.InputItem;
import com.workflowy.data.pojo.InputMetadata;
import com.workflowy.data.pojo.InputMirrorMetadata;
import cool.klass.data.store.DataStore;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorkflowyDataConverter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowyDataConverter.class);

    private static final long WORKFLOWY_EPOCH_OFFSET = 1262304000L;
    private static final Pattern FILE_DATE_PATTERN = Pattern.compile("\\.(\\d{4}-\\d{2}-\\d{2})\\.");

    private final ObjectMapper objectMapper;
    private final DataStore dataStore;
    private final File backupFile;

    private final MutableMap<String, Item> items = MapAdapter.adapt(new LinkedHashMap<>());
    private final MutableMap<String, Tag> tags = MapAdapter.adapt(new LinkedHashMap<>());
    private final ItemTagMappingList itemTagMappings = new ItemTagMappingList();
    private final MirrorList mirrors = new MirrorList();
    private final ItemDateList itemDates = new ItemDateList();

    private WorkflowyDataConverter(
            @Nonnull ObjectMapper objectMapper,
            @Nonnull DataStore dataStore,
            @Nonnull File backupFile)
    {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.dataStore = Objects.requireNonNull(dataStore);
        this.backupFile = Objects.requireNonNull(backupFile);
    }

    public static void convert(
            @Nonnull Path backupsPath,
            @Nonnull ObjectMapper objectMapper,
            @Nonnull DataStore dataStore,
            int daysLimit)
    {
        Instant highWatermark = WorkflowyDataConverter.getHighWatermark();

        ImmutableList<File> filesToProcess = WorkflowyDataConverter.getBackupFiles(backupsPath)
                .selectWith(WorkflowyDataConverter::isAfterHighWatermark, highWatermark)
                .take(daysLimit);

        if (filesToProcess.isEmpty())
        {
            LOGGER.info("No files to process after highWatermark {}", highWatermark);
            return;
        }

        LOGGER.info("Processing {} files after highWatermark {}", filesToProcess.size(), highWatermark);
        LOGGER.info("filesToProcess = {}", filesToProcess);

        filesToProcess
                .asLazy()
                .collect(file -> new WorkflowyDataConverter(objectMapper, dataStore, file))
                .forEach(WorkflowyDataConverter::processBackupFile);
    }

    private void processBackupFile()
    {
        try
        {
            processBackupFileOrThrow();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to process backup file: " + this.backupFile, e);
        }
    }

    private void processBackupFileOrThrow() throws IOException
    {
        LOGGER.info("Processing backup file: {}", this.backupFile);

        List<InputItem> rootItems = this.objectMapper.readValue(
                this.backupFile,
                new TypeReference<>()
                {
                });

        Instant backupInstant = getFileTimestamp(this.backupFile);

        LOGGER.info("Pass 1: Creating items from {} root items", rootItems.size());
        this.processItemsPass1(rootItems, null, 0);
        LOGGER.info("Created {} items", this.items.size());

        LOGGER.info("Pass 2: Extracting tags");
        this.extractTagsFromItems();
        LOGGER.info("Extracted {} tags and {} item-tag mappings", this.tags.size(), this.itemTagMappings.size());

        LOGGER.info("Pass 3: Processing metadata (mirrors, dates)");
        this.processMetadata(rootItems);
        LOGGER.info("Created {} mirrors and {} item dates", this.mirrors.size(), this.itemDates.size());

        this.mergeIntoDatabase(backupInstant);
    }

    private void processItemsPass1(List<InputItem> inputItems, String parentId, int startPriority)
    {
        int priority = startPriority;
        for (InputItem inputItem : inputItems)
        {
            Item item = this.createItem(inputItem, parentId, priority);
            this.items.put(inputItem.id(), item);
            priority++;

            if (inputItem.hasChildren())
            {
                this.processItemsPass1(inputItem.children(), inputItem.id(), 0);
            }
        }
    }

    private Item createItem(InputItem inputItem, String parentId, int priority)
    {
        Item item = new Item();
        item.setId(inputItem.id());
        item.setParentId(parentId);
        item.setName(inputItem.name() != null ? inputItem.name() : "");
        item.setNote(inputItem.note());
        item.setCompleted(inputItem.isCompleted());
        item.setCompletedAt(convertWorkflowyTimestamp(inputItem.completedTimestamp()));
        item.setPriority(priority);
        item.setCollapsed(false);

        return item;
    }

    private void extractTagsFromItems()
    {
        for (Item item : this.items.values())
        {
            this.extractTagsFromName(item);
        }
    }

    private void extractTagsFromName(Item item)
    {
        String name = item.getName();
        if (name == null || name.isEmpty())
        {
            return;
        }

        List<String> extractedTags = HashtagExtractor.extractHashtags(name);

        for (String tagName : extractedTags)
        {
            this.tags.computeIfAbsent(tagName, t ->
            {
                Tag newTag = new Tag();
                newTag.setName(t);
                newTag.setColor(null);
                return newTag;
            });

            ItemTagMapping mapping = new ItemTagMapping();
            mapping.setItemId(item.getId());
            mapping.setTagName(tagName);
            this.itemTagMappings.add(mapping);
        }
    }

    private void processMetadata(List<InputItem> inputItems)
    {
        for (InputItem inputItem : inputItems)
        {
            this.processItemMetadata(inputItem);
            if (inputItem.hasChildren())
            {
                this.processMetadata(inputItem.children());
            }
        }
    }

    private void processItemMetadata(InputItem inputItem)
    {
        InputMetadata metadata = inputItem.metadata();
        if (metadata == null)
        {
            return;
        }

        if (metadata.hasMirror())
        {
            this.processMirrorMetadata(inputItem.id(), metadata.mirror());
        }

        if (metadata.hasBacklink())
        {
            this.processBacklinkMetadata(metadata.backlink());
        }

        if (metadata.hasCalendar())
        {
            this.processCalendarMetadata(inputItem.id(), metadata.calendar());
        }
    }

    private void processMirrorMetadata(String itemId, InputMirrorMetadata mirrorMeta)
    {
        for (String sourceId : mirrorMeta.getMirrorSourceIds())
        {
            Mirror mirror = new Mirror();
            mirror.setId(UUID.randomUUID().toString());
            mirror.setSourceItemId(sourceId);
            mirror.setVirtualItemId(itemId);
            this.mirrors.add(mirror);
        }
    }

    private void processBacklinkMetadata(InputBacklinkMetadata backlinkMeta)
    {
        if (backlinkMeta.sourceId() != null && backlinkMeta.targetId() != null)
        {
            Mirror mirror = new Mirror();
            mirror.setId(UUID.randomUUID().toString());
            mirror.setSourceItemId(backlinkMeta.sourceId());
            mirror.setVirtualItemId(backlinkMeta.targetId());
            this.mirrors.add(mirror);
        }
    }

    private void processCalendarMetadata(String itemId, InputCalendarMetadata calendarMeta)
    {
        if (calendarMeta.date() != null)
        {
            Timestamp dateValue = parseCalendarDate(calendarMeta.date());
            if (dateValue != null)
            {
                ItemDate itemDate = new ItemDate();
                itemDate.setId(UUID.randomUUID().toString());
                itemDate.setItemId(itemId);
                itemDate.setDateValue(dateValue);
                this.itemDates.add(itemDate);
            }
        }
    }

    private void mergeIntoDatabase(Instant backupInstant)
    {
        long time = backupInstant.toEpochMilli();

        this.dataStore.runInTransaction(transaction ->
        {
            transaction.setSystemTime(time);

            LOGGER.info("Merging {} tags", this.tags.size());
            TagList existingTags = TagFinder.findMany(TagFinder.all());
            TagList updatedTags = new TagList();
            updatedTags.addAll(this.tags.values());
            TopLevelMergeOptions<Tag> tagMergeOptions = new TopLevelMergeOptions<>(TagFinder.getFinderInstance());
            existingTags.merge(updatedTags, tagMergeOptions);

            LOGGER.info("Merging {} items", this.items.size());
            ItemList existingItems = ItemFinder.findMany(ItemFinder.all());
            ItemList updatedItems = new ItemList();
            updatedItems.addAll(this.items.values());
            TopLevelMergeOptions<Item> itemMergeOptions = new TopLevelMergeOptions<>(ItemFinder.getFinderInstance());
            itemMergeOptions.doNotCompare(
                    ItemFinder.systemFrom(),
                    ItemFinder.systemTo(),
                    ItemFinder.createdById(),
                    ItemFinder.createdOn(),
                    ItemFinder.lastUpdatedById());
            existingItems.merge(updatedItems, itemMergeOptions);

            LOGGER.info("Merging {} item-tag mappings", this.itemTagMappings.size());
            ItemTagMappingList existingMappings = ItemTagMappingFinder.findMany(ItemTagMappingFinder.all());
            TopLevelMergeOptions<ItemTagMapping> mappingMergeOptions =
                    new TopLevelMergeOptions<>(ItemTagMappingFinder.getFinderInstance());
            existingMappings.merge(this.itemTagMappings, mappingMergeOptions);

            LOGGER.info("Merging {} mirrors", this.mirrors.size());
            MirrorList existingMirrors = MirrorFinder.findMany(MirrorFinder.all());
            TopLevelMergeOptions<Mirror> mirrorMergeOptions =
                    new TopLevelMergeOptions<>(MirrorFinder.getFinderInstance());
            existingMirrors.merge(this.mirrors, mirrorMergeOptions);

            LOGGER.info("Merging {} item dates", this.itemDates.size());
            ItemDateList existingDates = ItemDateFinder.findMany(ItemDateFinder.all());
            TopLevelMergeOptions<ItemDate> dateMergeOptions =
                    new TopLevelMergeOptions<>(ItemDateFinder.getFinderInstance());
            existingDates.merge(this.itemDates, dateMergeOptions);

            WorkflowyDataConverter.storeHighWatermark(backupInstant);

            return null;
        });

        LOGGER.info("Completed merge for backup file: {}", this.backupFile.getName());
    }

    private static Instant getHighWatermark()
    {
        Operation workflowyCriteria = DataImportTimestampFinder.name().eq("workflowy");
        DataImportTimestamp workflowyTimestamp = DataImportTimestampFinder.findOne(workflowyCriteria);

        Instant highWatermark = Optional.ofNullable(workflowyTimestamp)
                .map(DataImportTimestamp::getTimestamp)
                .map(Timestamp::toInstant)
                .orElse(Instant.MIN);

        LOGGER.info("High watermark: {}", highWatermark);
        return highWatermark;
    }

    private static void storeHighWatermark(@Nonnull Instant instant)
    {
        Timestamp highWatermark = Timestamp.from(instant);
        Operation workflowyCriteria = DataImportTimestampFinder.name().eq("workflowy");
        DataImportTimestamp workflowyTimestamp = DataImportTimestampFinder.findOne(workflowyCriteria);

        if (workflowyTimestamp == null)
        {
            DataImportTimestamp newTimestamp = new DataImportTimestamp();
            newTimestamp.setName("workflowy");
            newTimestamp.setTimestamp(highWatermark);
            newTimestamp.insert();
        }
        else
        {
            workflowyTimestamp.setTimestamp(highWatermark);
        }

        LOGGER.info("Stored high watermark: {}", instant);
    }

    private static ImmutableList<File> getBackupFiles(Path backupsPath)
    {
        File[] files = backupsPath.toFile().listFiles(
                pathname -> pathname.getName().endsWith(".workflowy.backup"));
        Objects.requireNonNull(files, backupsPath::toString);
        return ArrayAdapter.adapt(files).toSortedListBy(File::getName).toImmutable();
    }

    private static boolean isAfterHighWatermark(File file, Instant highWatermark)
    {
        Instant fileTimestamp = getFileTimestamp(file);
        return fileTimestamp.isAfter(highWatermark);
    }

    private static Instant getFileTimestamp(File file)
    {
        String fileName = file.getName();
        Matcher matcher = FILE_DATE_PATTERN.matcher(fileName);
        if (matcher.find())
        {
            LocalDate date = LocalDate.parse(matcher.group(1), DateTimeFormatter.ISO_LOCAL_DATE);
            return date.atStartOfDay().toInstant(ZoneOffset.UTC);
        }
        return Instant.MIN;
    }

    private static Timestamp convertWorkflowyTimestamp(Long workflowyTimestamp)
    {
        if (workflowyTimestamp == null)
        {
            return null;
        }
        long epochSeconds = workflowyTimestamp + WORKFLOWY_EPOCH_OFFSET;
        return Timestamp.from(Instant.ofEpochSecond(epochSeconds));
    }

    private static Timestamp parseCalendarDate(Object dateValue)
    {
        if (dateValue instanceof Number number)
        {
            long epochSeconds = number.longValue();
            return Timestamp.from(Instant.ofEpochSecond(epochSeconds));
        }
        return null;
    }
}
