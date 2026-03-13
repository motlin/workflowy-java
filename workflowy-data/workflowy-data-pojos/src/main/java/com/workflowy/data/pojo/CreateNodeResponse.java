package com.workflowy.data.pojo;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateNodeResponse(@JsonProperty("item_id") @Nonnull String itemId) {}
