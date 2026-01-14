package com.workflowy.dropwizard.application.cli;

import com.gs.fw.common.mithra.finder.Operation;
import com.workflowy.NodeContent;
import com.workflowy.NodeContentFinder;
import com.workflowy.NodeContentList;
import com.workflowy.dropwizard.application.WorkflowyApplication;
import com.workflowy.dropwizard.application.WorkflowyConfiguration;
import com.workflowy.dto.NodeContentDTO;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class ReadNodeCommand extends AbstractReadOnlyCommand {

	public ReadNodeCommand(WorkflowyApplication application) {
		super(application, "read-node", "Read a node by ID with optional child depth");
	}

	@Override
	public void configure(Subparser subparser) {
		super.configure(subparser);

		subparser
			.addArgument("--id")
			.type(String.class)
			.required(true)
			.help("Node ID (full UUID or first 12+ hex characters)");

		subparser
			.addArgument("--depth")
			.type(Integer.class)
			.setDefault(0)
			.choices(Arguments.range(0, 10))
			.help("Depth of children to include (0-10, default: 0)");
	}

	@Override
	protected Object executeCommand(Namespace namespace, WorkflowyConfiguration configuration) throws CommandException {
		String inputId = namespace.getString("id");
		int depth = namespace.getInt("depth");

		String fullId = this.resolveNodeId(inputId);

		// Find the node
		Operation operation = NodeContentFinder.id().eq(fullId);
		NodeContentList nodes = NodeContentFinder.findMany(operation);

		if (nodes.isEmpty()) {
			throw new CommandException("NOT_FOUND", "Node not found: " + fullId);
		}

		// Apply deep fetch based on depth
		NodeContentDTOMapper.applyDeepFetch(nodes, depth);

		NodeContent node = nodes.get(0);
		NodeContentDTO dto = NodeContentDTOMapper.toDTO(node, depth);

		return dto;
	}
}
