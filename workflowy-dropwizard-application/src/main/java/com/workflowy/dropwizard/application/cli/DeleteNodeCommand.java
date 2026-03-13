package com.workflowy.dropwizard.application.cli;

import com.workflowy.data.converter.WorkflowyApiClient;
import com.workflowy.dropwizard.application.WorkflowyApplication;
import com.workflowy.dropwizard.application.WorkflowyConfiguration;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class DeleteNodeCommand extends AbstractApiCommand {

	public DeleteNodeCommand(WorkflowyApplication application) {
		super(application, "delete-node", "Delete a node from Workflowy");
	}

	@Override
	public void configure(Subparser subparser) {
		super.configure(subparser);

		MutuallyExclusiveGroup nodeGroup = subparser.addMutuallyExclusiveGroup("node")
			.required(true);
		nodeGroup.addArgument("--id", "-i").type(String.class).help("Node ID");
		nodeGroup.addArgument("--path", "-p").type(String.class).help("Comma-separated path to node");
	}

	@Override
	protected void executeCommand(
		Namespace namespace,
		WorkflowyConfiguration configuration,
		WorkflowyApiClient apiClient
	) {
		String nodeId = this.resolveNodeId(
			namespace.getString("id"),
			namespace.getString("path"),
			apiClient
		);

		if (nodeId == null) {
			throw new IllegalArgumentException("Could not resolve node");
		}

		System.out.println("WARNING: This will permanently delete the node and all its children!");
		System.out.println("Deleting node: " + nodeId);

		apiClient.deleteNode(nodeId);

		System.out.println();
		System.out.println("Successfully deleted node");
	}
}
