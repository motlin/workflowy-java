package com.workflowy.data.pojo;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Tracks changes/attribution metadata for an item.
 * Each entry contains a "by" field (user ID) and dynamic fields mapping user IDs to timestamps.
 */
public record InputChangesMetadata(
	/** Created by - tracks who created this item. */
	@Nullable Map<String, Object> ct,
	/** Modified by - tracks who last modified this item. */
	@Nullable Map<String, Object> mn
) {}
