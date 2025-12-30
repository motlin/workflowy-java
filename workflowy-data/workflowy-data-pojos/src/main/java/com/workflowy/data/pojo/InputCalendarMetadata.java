package com.workflowy.data.pojo;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Calendar/date metadata attached to an item.
 */
public record InputCalendarMetadata(
        @JsonProperty("date")
        @Nullable
        Object date,

        @JsonProperty("root")
        @Nullable
        Boolean isRoot
)
{
}
