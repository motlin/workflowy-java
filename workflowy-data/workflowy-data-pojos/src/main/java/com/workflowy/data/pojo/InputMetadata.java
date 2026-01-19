package com.workflowy.data.pojo;

import java.util.Map;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InputMetadata(
	@Nullable InputMirrorMetadata mirror,

	@Nullable InputBacklinkMetadata backlink,

	@Nullable InputCalendarMetadata calendar,

	@Nullable InputS3FileMetadata s3File,

	@Nullable InputAiMetadata ai,

	@Nullable String originalId,

	@Nullable Boolean isVirtualRoot,

	@Nullable Boolean isReferencesRoot,

	@Nullable String layoutMode,

	@Nullable Map<String, Boolean> virtualRootIds,

	@Nullable Map<String, Object> changes,

	@JsonProperty("numbered_start") @Nullable Integer numberedStart
) {

	public boolean hasMirror() {
		return this.mirror != null;
	}

	public boolean hasBacklink() {
		return this.backlink != null;
	}

	public boolean hasCalendar() {
		return this.calendar != null;
	}
}
