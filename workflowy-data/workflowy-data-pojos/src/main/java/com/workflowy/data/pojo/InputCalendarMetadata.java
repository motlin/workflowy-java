package com.workflowy.data.pojo;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InputCalendarMetadata(
	@JsonProperty("root") @Nullable Boolean isRoot,

	@Nullable String level,

	@Nullable InputCalendarLevelsMetadata levels,

	@Nullable String value,

	@Nullable Integer dateId,

	@Nullable Long timestamp,

	@JsonProperty("found_dates") @Nullable Boolean foundDates
) {}
