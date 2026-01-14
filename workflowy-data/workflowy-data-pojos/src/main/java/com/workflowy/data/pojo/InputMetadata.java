package com.workflowy.data.pojo;

import java.util.Map;
import javax.annotation.Nullable;

// TODO 2025-12-30: JsonProperty is not necessary in java record DTOs
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Metadata attached to a Workflowy item.
 * Contains optional fields for mirrors, backlinks, calendar dates, S3 files, and AI data.
 */
public record InputMetadata(
        @JsonProperty("mirror")
        @Nullable
        InputMirrorMetadata mirror,

        @JsonProperty("backlink")
        @Nullable
        InputBacklinkMetadata backlink,

        @JsonProperty("calendar")
        @Nullable
        InputCalendarMetadata calendar,

        @JsonProperty("s3File")
        @Nullable
        InputS3FileMetadata s3File,

        @JsonProperty("ai")
        @Nullable
        InputAiMetadata ai,

        @JsonProperty("originalId")
        @Nullable
        String originalId,

        @JsonProperty("isVirtualRoot")
        @Nullable
        Boolean isVirtualRoot,

        @JsonProperty("isReferencesRoot")
        @Nullable
        Boolean isReferencesRoot,

        @JsonProperty("layoutMode")
        @Nullable
        String layoutMode,

        @JsonProperty("virtualRootIds")
        @Nullable
        Map<String, Boolean> virtualRootIds,

        @JsonProperty("changes")
        @Nullable
        Map<String, Object> changes
)
{
    public static InputMetadata empty()
    {
        return new InputMetadata(null, null, null, null, null, null, null, null, null, null, null);
    }

    public boolean hasMirror()
    {
        return this.mirror != null;
    }

    public boolean hasBacklink()
    {
        return this.backlink != null;
    }

    public boolean hasCalendar()
    {
        return this.calendar != null;
    }
}
