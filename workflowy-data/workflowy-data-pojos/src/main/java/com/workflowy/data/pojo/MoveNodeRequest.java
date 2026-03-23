package com.workflowy.data.pojo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MoveNodeRequest(@JsonProperty("parent_id") @Nonnull String parentId, @Nullable String position) {}
