package com.workflowy.dropwizard.application.cli;

import java.util.List;

import com.gs.fw.common.mithra.finder.Operation;
import com.workflowy.NodeContentFinder;
import com.workflowy.NodeContentList;
import com.workflowy.dropwizard.application.WorkflowyApplication;
import com.workflowy.dropwizard.application.WorkflowyConfiguration;
import com.workflowy.dto.NodeContentDTO;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class ListByIdCommand extends AbstractReadOnlyCommand
{
    public ListByIdCommand(WorkflowyApplication application)
    {
        super(application, "list-by-id", "List children of a parent node by ID");
    }

    @Override
    public void configure(Subparser subparser)
    {
        super.configure(subparser);

        subparser.addArgument("--parent-id")
                .type(String.class)
                .required(false)
                .help("Parent node ID (omit for root nodes)");
    }

    @Override
    protected Object executeCommand(Namespace namespace, WorkflowyConfiguration configuration)
            throws CommandException
    {
        String parentId = namespace.getString("parent_id");

        Operation operation;
        if (parentId == null)
        {
            // List root nodes (parentId is null)
            operation = NodeContentFinder.parentId().isNull();
        }
        else
        {
            String fullParentId = this.resolveNodeId(parentId);
            operation = NodeContentFinder.parentId().eq(fullParentId);
        }

        NodeContentList nodes = NodeContentFinder.findMany(operation);

        // Deep fetch metadata for all nodes (depth 0 = no children)
        NodeContentDTOMapper.applyDeepFetch(nodes, 0);

        List<NodeContentDTO> dtos = NodeContentDTOMapper.toDTOList(nodes, 0);

        return dtos;
    }
}
