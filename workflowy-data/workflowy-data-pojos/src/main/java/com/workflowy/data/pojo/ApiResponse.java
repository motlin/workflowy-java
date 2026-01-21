package com.workflowy.data.pojo;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Response wrapper for Workflowy API endpoints that return nodes.
 *
 * <p>Used by:
 * <ul>
 *   <li>GET /api/v1/nodes-export - Returns all nodes as a flat list</li>
 *   <li>GET /api/v1/nodes - Returns root nodes</li>
 *   <li>GET /api/v1/nodes?parent_id=X - Returns children of a node</li>
 * </ul>
 */
public record ApiResponse(@Nullable List<ApiInputItem> nodes) {
	public ApiResponse {
		if (nodes == null) {
			nodes = List.of();
		}
	}

	@Nonnull
	public List<ApiInputItem> getNodesOrEmpty() {
		return this.nodes != null ? this.nodes : List.of();
	}
}
