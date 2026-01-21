package com.workflowy.data.pojo;

import javax.annotation.Nullable;

public record ApiInputData(
	@Nullable String layoutMode,

	@Nullable InputAiMetadata ai,

	@Nullable Boolean isReferencesRoot
) {}
