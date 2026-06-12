package com.workflowy.dropwizard.application.cli;

import java.time.Instant;

import com.gs.fw.common.mithra.finder.Operation;
import com.workflowy.NodeContentFinder;
import com.workflowy.NodeContentList;
import com.workflowy.NodeMetadataFinder;
import com.workflowy.NodeMetadataList;
import com.workflowy.dropwizard.application.WorkflowyApplication;
import com.workflowy.dropwizard.application.WorkflowyConfiguration;
import net.sourceforge.argparse4j.inf.Namespace;

public class CacheStatusCommand extends AbstractReadOnlyCommand {

	public CacheStatusCommand(WorkflowyApplication application) {
		super(application, "cache-status", "Display cache and database statistics");
	}

	@Override
	protected Object executeCommand(Namespace namespace, WorkflowyConfiguration configuration) throws CommandException {
		var status = new CacheStatusDto();
		status.setQueryTime(Instant.now());
		status.setDatabaseType("H2");

		// Count total nodes
		Operation allNodesOp = NodeContentFinder.all();
		NodeContentList allNodes = NodeContentFinder.findMany(allNodesOp);
		status.setTotalNodes(allNodes.size());

		// Count root nodes (parentId is null)
		Operation rootNodesOp = NodeContentFinder.parentId().isNull();
		NodeContentList rootNodes = NodeContentFinder.findMany(rootNodesOp);
		status.setRootNodes(rootNodes.size());

		Operation completedOp = NodeMetadataFinder.completedAt().isNotNull();
		NodeMetadataList completedMetadata = NodeMetadataFinder.findMany(completedOp);
		status.setCompletedNodes(completedMetadata.size());

		return status;
	}
}
