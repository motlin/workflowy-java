package com.workflowy.data.pojo;

import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * Represents a single Workflowy item from the backup JSON.
 */
public record InputItem(
	/** UUID string. */
	String id,

	/** Text content, may contain HTML and #hashtags. */
	@JsonProperty("nm") String name,

	@JsonProperty("no") @Nullable String note,

	/** Workflowy format - seconds since Jan 1, 2010. */
	@JsonProperty("ct") @Nullable Long createdTimestamp,

	/** Workflowy format - seconds since Jan 1, 2010. */
	@JsonProperty("lm") @Nullable Long lastModifiedTimestamp,

	/** Workflowy format - seconds since Jan 1, 2010. */
	@JsonProperty("cp") @Nullable Long completedTimestamp,

	@Nullable InputMetadata metadata,

	@JsonSetter(nulls = Nulls.AS_EMPTY)
	@JsonProperty("ch") List<InputItem> children
) {

	public boolean isCompleted() {
		return this.completedTimestamp != null;
	}

	public boolean hasChildren() {
		return Iterate.notEmpty(this.children);
	}
}
