package com.workflowy.data.pojo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateNodeRequest(
	@JsonProperty("parent_id") @Nonnull String parentId,
	@Nonnull String name,
	@Nullable String note,
	@Nullable String layoutMode,
	@Nullable String position
) {}
