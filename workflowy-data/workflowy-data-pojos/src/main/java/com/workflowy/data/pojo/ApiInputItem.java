package com.workflowy.data.pojo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApiInputItem(
	@Nonnull String id,

	@Nonnull String name,

	@Nullable String note,

	@JsonProperty("parent_id") @Nullable String parentId,

	int priority,

	boolean completed,

	@Nullable Long createdAt,

	@Nullable Long modifiedAt,

	@Nullable Long completedAt,

	@Nullable ApiInputData data
) {}
