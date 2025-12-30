package com.workflowy.data.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Backlink metadata indicating a link between two items.
 */
public record InputBacklinkMetadata(
        @JsonProperty("sourceID")
        String sourceId,

        @JsonProperty("targetID")
        String targetId
)
{
}
