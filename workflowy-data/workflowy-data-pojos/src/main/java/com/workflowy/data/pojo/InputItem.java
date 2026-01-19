package com.workflowy.data.pojo;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * Represents a single Workflowy item from the backup JSON.
 */
public record InputItem(
	/** UUID string. */
	@JsonProperty("id") String id,

	/** Text content, may contain HTML and #hashtags. */
	@JsonProperty("nm") String name,

	@JsonProperty("no") @Nullable String note,

	/** Workflowy format - seconds since Jan 1, 2010. */
	@JsonProperty("ct") @Nullable Long createdTimestamp,

	/** Workflowy format - seconds since Jan 1, 2010. */
	@JsonProperty("lm") @Nullable Long lastModifiedTimestamp,

	/** Workflowy format - seconds since Jan 1, 2010. */
	@JsonProperty("cp") @Nullable Long completedTimestamp,

	@JsonProperty("metadata") @Nonnull InputMetadata metadata,

	@JsonProperty("ch") @Nullable List<InputItem> children
) {
	public InputItem {
		if (metadata == null) {
			metadata = InputMetadata.empty();
		}
		if (children == null) {
			children = List.of();
		}
	}

	public boolean isCompleted() {
		return this.completedTimestamp != null;
	}

	public boolean hasChildren() {
		return Iterate.notEmpty(this.children);
	}
}
