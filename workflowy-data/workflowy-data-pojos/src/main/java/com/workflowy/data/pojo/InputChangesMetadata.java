package com.workflowy.data.pojo;

import java.util.Map;

import javax.annotation.Nullable;

// TODO 2026-01-20: Use JsonProperty to map "ct" and "mn" to more descriptive field names.
public record InputChangesMetadata(
	/** Created by - tracks who created this item. */
	@Nullable Map<String, Object> ct,
	/** Modified by - tracks who last modified this item. */
	@Nullable Map<String, Object> mn
) {}
