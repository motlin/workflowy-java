package com.workflowy.data.pojo;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI-related metadata for an item.
 */
public record InputAiMetadata(
        @JsonProperty("inChat")
        @Nullable
        Boolean inChat
)
{
}
