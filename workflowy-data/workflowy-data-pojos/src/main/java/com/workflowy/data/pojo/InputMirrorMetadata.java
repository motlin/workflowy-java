package com.workflowy.data.pojo;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

public record InputMirrorMetadata(
	@Nullable String originalId,

	@Nullable Boolean isMirrorRoot,

	@JsonSetter(nulls = Nulls.AS_EMPTY) Map<String, Boolean> mirrorRootIds,

	@JsonSetter(nulls = Nulls.AS_EMPTY) Map<String, Boolean> backlinkMirrorRootIds
) {
	public Set<String> getMirrorSourceIds() {
		return this.mirrorRootIds.keySet();
	}

	public Set<String> getBacklinkMirrorIds() {
		return this.backlinkMirrorRootIds.keySet();
	}
}
