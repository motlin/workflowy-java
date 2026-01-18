package com.workflowy.data.pojo;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data field from Workflowy API export nodes.
 *
 * <p>Contains optional layout and metadata fields:
 * <ul>
 *   <li>layoutMode: Display mode (e.g., "board")</li>
 *   <li>ai: AI-related metadata</li>
 *   <li>isReferencesRoot: Whether this node is a references root</li>
 * </ul>
 *
 * <p>Note: This is the "data" field from the API export format,
 * which differs from the "metadata" field in the backup format.
 */
public record ApiInputData(
	@JsonProperty("layoutMode") @Nullable String layoutMode,

	@JsonProperty("ai") @Nullable InputAiMetadata ai,

	@JsonProperty("isReferencesRoot") @Nullable Boolean isReferencesRoot
) {
	public static ApiInputData empty() {
		return new ApiInputData(null, null, null);
	}
}
