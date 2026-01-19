package com.workflowy.data.pojo;

import javax.annotation.Nullable;

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
	@Nullable String layoutMode,

	@Nullable InputAiMetadata ai,

	@Nullable Boolean isReferencesRoot
) {
	public static ApiInputData empty() {
		return new ApiInputData(null, null, null);
	}
}
