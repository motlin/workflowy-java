package com.workflowy.data.pojo;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Calendar/date metadata attached to an item.
 */
public record InputCalendarMetadata(
        @Nullable
        Object date,

        @JsonProperty("root")
        @Nullable
        Boolean isRoot,

        @Nullable
        String level,

        @Nullable
        Object levels,

        @Nullable
        Object value,

        @Nullable
        String dateId,

        @Nullable
        Long timestamp
)
{
}
