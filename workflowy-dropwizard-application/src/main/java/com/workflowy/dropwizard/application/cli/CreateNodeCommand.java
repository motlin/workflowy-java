package com.workflowy.dropwizard.application.cli;

import com.workflowy.data.converter.WorkflowyApiClient;
import com.workflowy.data.pojo.ApiInputItem;
import com.workflowy.data.pojo.CreateNodeRequest;
import com.workflowy.dropwizard.application.WorkflowyApplication;
import com.workflowy.dropwizard.application.WorkflowyConfiguration;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class CreateNodeCommand extends AbstractApiCommand {

	public CreateNodeCommand(WorkflowyApplication application) {
		super(application, "create-node", "Create a new node in Workflowy");
	}

	@Override
	public void configure(Subparser subparser) {
		super.configure(subparser);

		subparser
			.addArgument("--name", "-n")
			.type(String.class)
			.required(true)
			.help("Name of the new node");

		MutuallyExclusiveGroup parentGroup = subparser.addMutuallyExclusiveGroup("parent")
			.required(true);
		parentGroup
			.addArgument("--parent-id")
			.type(String.class)
			.help("Parent node ID (full UUID or short ID)");
		parentGroup
			.addArgument("--parent-path")
			.type(String.class)
			.help("Comma-separated path to parent node");

		subparser
			.addArgument("--note")
			.type(String.class)
			.help("Note text for the new node");

		subparser
			.addArgument("--layout-mode")
			.type(String.class)
			.help("Layout mode (e.g., document, list, board)");

		subparser
			.addArgument("--position")
			.type(String.class)
			.choices("top", "bottom")
			.setDefault("bottom")
			.help("Position within parent: top or bottom (default: bottom)");
	}

	@Override
	protected void executeCommand(
		Namespace namespace,
		WorkflowyConfiguration configuration,
		WorkflowyApiClient apiClient
	) {
		String name = namespace.getString("name");
		String parentId = this.resolveNodeId(
			namespace.getString("parent_id"),
			namespace.getString("parent_path"),
			apiClient
		);

		if (parentId == null) {
			throw new IllegalArgumentException("Could not resolve parent node");
		}

		String note = namespace.getString("note");
		String layoutMode = namespace.getString("layout_mode");
		String position = namespace.getString("position");

		var request = new CreateNodeRequest(parentId, name, note, layoutMode, position);

		System.out.println("Creating node: " + name);
		System.out.println("Parent: " + parentId);

		String newNodeId = apiClient.createNode(request);

		ApiInputItem created = apiClient.getNode(newNodeId);

		System.out.println();
		System.out.println("Successfully created node");
		System.out.println("  ID: " + created.id());
		System.out.println("  Name: " + created.name());
		System.out.println("  URL: " + getWorkflowyUrl(created.id()));
	}
}
