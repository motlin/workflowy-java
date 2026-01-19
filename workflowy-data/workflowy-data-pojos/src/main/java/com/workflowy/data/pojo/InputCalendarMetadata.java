package com.workflowy.data.pojo;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Calendar/date metadata attached to an item.
 */
public record InputCalendarMetadata(
	@Nullable Boolean date,

	@JsonProperty("root") @Nullable Boolean isRoot,

	@Nullable String level,

	@Nullable Integer levels,

	@Nullable Integer value,

	@Nullable String dateId,

	@Nullable Long timestamp,

	@JsonProperty("found_dates") @Nullable Boolean foundDates
) {}
