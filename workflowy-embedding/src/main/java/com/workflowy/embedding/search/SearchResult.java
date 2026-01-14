package com.workflowy.embedding.search;

import javax.annotation.Nullable;

public class SearchResult {

	private final String nodeId;
	private final double distance;

	@Nullable
	private String name;

	@Nullable
	private String note;

	@Nullable
	private String fullPath;

	@Nullable
	private String textContent;

	public SearchResult(String nodeId, double distance) {
		this.nodeId = nodeId;
		this.distance = distance;
	}

	public String getNodeId() {
		return this.nodeId;
	}

	public double getDistance() {
		return this.distance;
	}

	public double getSimilarity() {
		return 1.0 - this.distance;
	}

	@Nullable
	public String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getNote() {
		return this.note;
	}

	public void setNote(@Nullable String note) {
		this.note = note;
	}

	@Nullable
	public String getFullPath() {
		return this.fullPath;
	}

	public void setFullPath(@Nullable String fullPath) {
		this.fullPath = fullPath;
	}

	@Nullable
	public String getTextContent() {
		return this.textContent;
	}

	public void setTextContent(@Nullable String textContent) {
		this.textContent = textContent;
	}

	@Override
	public String toString() {
		return (
			"SearchResult{"
			+ "nodeId='"
			+ this.nodeId
			+ '\''
			+ ", distance="
			+ this.distance
			+ ", name='"
			+ this.name
			+ '\''
			+ ", fullPath='"
			+ this.fullPath
			+ '\''
			+ '}'
		);
	}
}
