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
import net.sourceforge.argparse4j.inf.Subparser;

public class CacheStatusCommand extends AbstractReadOnlyCommand {

	public CacheStatusCommand(WorkflowyApplication application) {
		super(application, "cache-status", "Display cache and database statistics");
	}

	@Override
	public void configure(Subparser subparser) {
		super.configure(subparser);
		// No additional arguments required
	}

	@Override
	protected Object executeCommand(Namespace namespace, WorkflowyConfiguration configuration) throws CommandException {
		CacheStatusDto status = new CacheStatusDto();
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

		// Count completed nodes
		Operation completedOp = NodeMetadataFinder.completed().eq(true);
		NodeMetadataList completedMetadata = NodeMetadataFinder.findMany(completedOp);
		status.setCompletedNodes(completedMetadata.size());

		return status;
	}
}
