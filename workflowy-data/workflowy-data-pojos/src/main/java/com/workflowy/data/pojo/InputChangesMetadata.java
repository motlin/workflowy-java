package com.workflowy.data.pojo;

import javax.annotation.Nullable;

/**
 * Tracks changes/attribution metadata for an item.
 */
public record InputChangesMetadata(
	/** Created by - tracks who created this item. */
	@Nullable InputChangeEntry ct
) {}
