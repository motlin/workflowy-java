package com.workflowy.dropwizard.application.cli;

import java.util.List;

import com.gs.fw.common.mithra.finder.Operation;
import com.workflowy.NodeContent;
import com.workflowy.NodeContentFinder;
import com.workflowy.NodeContentList;
import com.workflowy.dropwizard.application.WorkflowyApplication;
import com.workflowy.dropwizard.application.WorkflowyConfiguration;
import com.workflowy.dto.NodeContentDTO;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class ListByPathCommand extends AbstractReadOnlyCommand {

	public ListByPathCommand(WorkflowyApplication application) {
		super(application, "list-by-path", "Navigate to a path and list children");
	}

	@Override
	public void configure(Subparser subparser) {
		super.configure(subparser);

		subparser
			.addArgument("--root-id")
			.type(String.class)
			.required(false)
			.help("Starting node ID (optional, defaults to root)");

		subparser
			.addArgument("--path")
			.type(String.class)
			.required(true)
			.help("Comma-separated path of node names to navigate");
	}

	@Override
	protected Object executeCommand(Namespace namespace, WorkflowyConfiguration configuration) throws CommandException {
		String rootId = namespace.getString("root_id");
		String pathString = namespace.getString("path");

		String[] pathParts = pathString.split(",");

		// Start from root or specified node
		String currentParentId = rootId != null ? this.resolveNodeId(rootId) : null;

		// Navigate down the path
		for (String pathPart : pathParts) {
			String targetName = pathPart.trim();

			Operation operation;
			if (currentParentId == null) {
				operation = NodeContentFinder.parentId().isNull().and(NodeContentFinder.name().eq(targetName));
			} else {
				operation = NodeContentFinder.parentId()
					.eq(currentParentId)
					.and(NodeContentFinder.name().eq(targetName));
			}

			NodeContent matchingNode = NodeContentFinder.findOne(operation);

			if (matchingNode == null) {
				throw new CommandException(
					"PATH_NOT_FOUND",
					"Path segment not found: "
					+ targetName
					+ " (at parent: "
					+ (currentParentId != null ? currentParentId : "root")
					+ ")"
				);
			}

			currentParentId = matchingNode.getId();
		}

		// List children at final path location
		Operation childrenOp = NodeContentFinder.parentId().eq(currentParentId);
		NodeContentList children = NodeContentFinder.findMany(childrenOp);

		// Deep fetch metadata
		NodeContentDTOMapper.applyDeepFetch(children, 0);

		List<NodeContentDTO> dtos = NodeContentDTOMapper.toDTOList(children, 0);

		return dtos;
	}
}
