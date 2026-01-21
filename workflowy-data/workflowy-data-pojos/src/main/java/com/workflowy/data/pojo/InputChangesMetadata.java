package com.workflowy.data.pojo;

import java.util.Map;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InputChangesMetadata(
	/** Created by - tracks who created this item. */
	@JsonProperty("ct") @Nullable Map<String, Object> createdBy,
	/** Modified by - tracks who last modified this item. */
	@JsonProperty("mn") @Nullable Map<String, Object> modifiedBy,
	/** Completed by - tracks who completed this item. */
	@JsonProperty("cp") @Nullable Map<String, Object> completedBy
) {}
