package com.workflowy.data.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InputBacklinkMetadata(
	@JsonProperty("sourceID") String sourceId,

	@JsonProperty("targetID") String targetId
) {}
