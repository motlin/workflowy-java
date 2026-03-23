package com.workflowy.dropwizard.application.cli;

import com.workflowy.data.converter.WorkflowyApiClient;
import com.workflowy.data.pojo.ApiInputItem;
import com.workflowy.data.pojo.UpdateNodeRequest;
import com.workflowy.dropwizard.application.WorkflowyApplication;
import com.workflowy.dropwizard.application.WorkflowyConfiguration;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class UpdateNodeCommand extends AbstractApiCommand {

	public UpdateNodeCommand(WorkflowyApplication application) {
		super(application, "update-node", "Update an existing node in Workflowy");
	}

	@Override
	public void configure(Subparser subparser) {
		super.configure(subparser);

		MutuallyExclusiveGroup nodeGroup = subparser.addMutuallyExclusiveGroup("node").required(true);
		nodeGroup.addArgument("--id", "-i").type(String.class).help("Node ID");
		nodeGroup.addArgument("--path", "-p").type(String.class).help("Comma-separated path to node");

		subparser.addArgument("--name", "-n").type(String.class).help("New name");
		subparser.addArgument("--note").type(String.class).help("New note text");
		subparser
			.addArgument("--clear-note")
			.action(Arguments.storeTrue())
			.setDefault(false)
			.help("Clear the note (set to empty)");
		subparser.addArgument("--layout-mode").type(String.class).help("New layout mode");
	}

	@Override
	protected void executeCommand(
		Namespace namespace,
		WorkflowyConfiguration configuration,
		WorkflowyApiClient apiClient
	) {
		String name = namespace.getString("name");
		String note = namespace.getString("note");
		boolean clearNote = namespace.getBoolean("clear_note");
		String layoutMode = namespace.getString("layout_mode");

		if (name == null && note == null && !clearNote && layoutMode == null) {
			throw new IllegalArgumentException(
				"At least one of --name, --note, --clear-note, or --layout-mode is required"
			);
		}

		String nodeId = this.resolveNodeId(namespace.getString("id"), namespace.getString("path"), apiClient);

		if (nodeId == null) {
			throw new IllegalArgumentException("Could not resolve node");
		}

		String effectiveNote = clearNote ? "" : note;
		var request = new UpdateNodeRequest(name, effectiveNote, layoutMode);

		System.out.println("Updating node: " + nodeId);

		apiClient.updateNode(nodeId, request);
		ApiInputItem updated = apiClient.getNode(nodeId);

		System.out.println();
		System.out.println("Successfully updated node");
		System.out.println("  ID: " + updated.id());
		System.out.println("  Name: " + updated.name());
		System.out.println("  URL: " + getWorkflowyUrl(updated.id()));
	}
}
