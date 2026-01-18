package com.workflowy.data.pojo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single Workflowy node from the API export endpoint.
 *
 * <p>The API export format differs from the backup format:
 * <ul>
 *   <li>Flat structure with explicit parent_id (no nested children)</li>
 *   <li>Full field names (name, note, priority) instead of abbreviations (nm, no)</li>
 *   <li>Unix timestamps (seconds since 1970) instead of Workflowy timestamps (seconds since Jan 1, 2010)</li>
 *   <li>Has explicit 'completed' boolean field</li>
 *   <li>Uses 'data' field for metadata instead of 'metadata'</li>
 * </ul>
 *
 * <p>API endpoint: GET https://workflowy.com/api/v1/nodes-export
 * (Rate limited: 1 request per minute)
 *
 * @see InputItem for the backup format equivalent
 */
public record ApiInputItem(
	@JsonProperty("id") @Nonnull String id,

	@JsonProperty("name") @Nonnull String name,

	@JsonProperty("note") @Nullable String note,

	@JsonProperty("parent_id") @Nullable String parentId,

	@JsonProperty("priority") int priority,

	@JsonProperty("completed") boolean completed,

	@JsonProperty("createdAt") @Nullable Long createdAt,

	@JsonProperty("modifiedAt") @Nullable Long modifiedAt,

	@JsonProperty("completedAt") @Nullable Long completedAt,

	@JsonProperty("data") @Nullable ApiInputData data
) {
	public ApiInputItem {
		if (data == null) {
			data = ApiInputData.empty();
		}
	}

	/**
	 * @return The layout mode from the data field, or null if not set.
	 */
	@Nullable
	public String layoutMode() {
		return this.data != null ? this.data.layoutMode() : null;
	}

	/**
	 * @return Whether the node is a references root, defaulting to false if not set.
	 */
	public boolean isReferencesRoot() {
		return this.data != null && Boolean.TRUE.equals(this.data.isReferencesRoot());
	}

	/**
	 * @return Whether the node is in an AI chat, defaulting to false if not set.
	 */
	public boolean isInChat() {
		return this.data != null && this.data.ai() != null && Boolean.TRUE.equals(this.data.ai().inChat());
	}
}
