package com.workflowy.data.pojo;

import javax.annotation.Nullable;

/**
 * Represents a single change entry with user attribution.
 */
public record InputChangeEntry(
	@Nullable Long by
) {}
