package com.workflowy.data.pojo;

import javax.annotation.Nullable;

/**
 * Specifies which calendar time levels are enabled for a date item.
 */
public record InputCalendarLevelsMetadata(
	@Nullable Boolean day,
	@Nullable Boolean week,
	@Nullable Boolean month,
	@Nullable Boolean year
) {}
