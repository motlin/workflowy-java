package com.workflowy.dropwizard.application.cli;

import java.time.Instant;

public class CacheStatusDto {

	private long totalNodes;
	private long rootNodes;
	private long completedNodes;
	private String databaseType;
	private Instant queryTime;

	public long getTotalNodes() {
		return this.totalNodes;
	}

	public void setTotalNodes(long totalNodes) {
		this.totalNodes = totalNodes;
	}

	public long getRootNodes() {
		return this.rootNodes;
	}

	public void setRootNodes(long rootNodes) {
		this.rootNodes = rootNodes;
	}

	public long getCompletedNodes() {
		return this.completedNodes;
	}

	public void setCompletedNodes(long completedNodes) {
		this.completedNodes = completedNodes;
	}

	public String getDatabaseType() {
		return this.databaseType;
	}

	public void setDatabaseType(String databaseType) {
		this.databaseType = databaseType;
	}

	public Instant getQueryTime() {
		return this.queryTime;
	}

	public void setQueryTime(Instant queryTime) {
		this.queryTime = queryTime;
	}
}
