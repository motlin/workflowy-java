package com.workflowy.data.pojo;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single Workflowy item from the backup JSON.
 *
 * <p>JSON fields:
 * <ul>
 *   <li>id: UUID string</li>
 *   <li>nm: name (text content, may contain HTML and #hashtags)</li>
 *   <li>no: note (optional description)</li>
 *   <li>ct: created timestamp (Workflowy format - seconds since Jan 1, 2010)</li>
 *   <li>lm: last modified timestamp</li>
 *   <li>cp: completed timestamp (if completed)</li>
 *   <li>metadata: object containing mirror, backlink, calendar, s3File, ai data</li>
 *   <li>ch: children array (recursive)</li>
 * </ul>
 */
public record InputItem(
        @JsonProperty("id")
        String id,

        @JsonProperty("nm")
        String name,

        @JsonProperty("no")
        @Nullable
        String note,

        @JsonProperty("ct")
        @Nullable
        Long createdTimestamp,

        @JsonProperty("lm")
        @Nullable
        Long lastModifiedTimestamp,

        @JsonProperty("cp")
        @Nullable
        Long completedTimestamp,

        @JsonProperty("metadata")
        @Nonnull
        InputMetadata metadata,

        @JsonProperty("ch")
        @Nullable
        List<InputItem> children
)
{
    public InputItem
    {
        if (metadata == null)
        {
            metadata = InputMetadata.empty();
        }
        if (children == null)
        {
            children = List.of();
        }
    }

    public boolean isCompleted()
    {
        return completedTimestamp != null;
    }

    public boolean hasChildren()
    {
        return children != null && !children.isEmpty();
    }
}
