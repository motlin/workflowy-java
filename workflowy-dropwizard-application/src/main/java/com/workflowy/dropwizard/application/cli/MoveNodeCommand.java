package com.workflowy.dropwizard.application.cli;

import com.workflowy.data.converter.WorkflowyApiClient;
import com.workflowy.data.pojo.ApiInputItem;
import com.workflowy.dropwizard.application.WorkflowyApplication;
import com.workflowy.dropwizard.application.WorkflowyConfiguration;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class MoveNodeCommand extends AbstractApiCommand {

	public MoveNodeCommand(WorkflowyApplication application) {
		super(application, "move-node", "Move a node to a new parent in Workflowy");
	}

	@Override
	public void configure(Subparser subparser) {
		super.configure(subparser);

		MutuallyExclusiveGroup nodeGroup = subparser.addMutuallyExclusiveGroup("node").required(true);
		nodeGroup.addArgument("--node-id").type(String.class).help("Node ID to move");
		nodeGroup.addArgument("--node-path").type(String.class).help("Comma-separated path to node to move");

		MutuallyExclusiveGroup parentGroup = subparser.addMutuallyExclusiveGroup("parent").required(true);
		parentGroup.addArgument("--parent-id").type(String.class).help("Destination parent node ID");
		parentGroup.addArgument("--parent-path").type(String.class).help("Comma-separated path to destination parent");

		subparser
			.addArgument("--position")
			.type(String.class)
			.choices("top", "bottom")
			.help("Position within new parent: top or bottom");
	}

	@Override
	protected void executeCommand(
		Namespace namespace,
		WorkflowyConfiguration configuration,
		WorkflowyApiClient apiClient
	) {
		String nodeId = this.resolveNodeId(namespace.getString("node_id"), namespace.getString("node_path"), apiClient);

		String newParentId = this.resolveNodeId(
			namespace.getString("parent_id"),
			namespace.getString("parent_path"),
			apiClient
		);

		if (nodeId == null) {
			throw new IllegalArgumentException("Could not resolve source node");
		}
		if (newParentId == null) {
			throw new IllegalArgumentException("Could not resolve destination parent");
		}

		String position = namespace.getString("position");

		System.out.println("Moving node: " + nodeId);
		System.out.println("  To parent: " + newParentId);
		if (position != null) {
			System.out.println("  Position: " + position);
		}

		apiClient.moveNode(nodeId, newParentId, position);
		ApiInputItem moved = apiClient.getNode(nodeId);

		System.out.println();
		System.out.println("Successfully moved node");
		System.out.println("  ID: " + moved.id());
		System.out.println("  Name: " + moved.name());
		System.out.println("  URL: " + getWorkflowyUrl(moved.id()));
	}
}
