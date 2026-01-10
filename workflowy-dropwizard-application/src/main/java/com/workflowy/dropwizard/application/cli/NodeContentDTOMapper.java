package com.workflowy.dropwizard.application.cli;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.workflowy.NodeContent;
import com.workflowy.NodeContentFinder;
import com.workflowy.NodeContentList;
import com.workflowy.NodeMetadata;
import com.workflowy.dto.NodeContentDTO;
import com.workflowy.dto.NodeMetadataDTO;

public final class NodeContentDTOMapper
{
    private NodeContentDTOMapper()
    {
        throw new AssertionError("Utility class");
    }

    public static NodeContentDTO toDTO(NodeContent content, int depth)
    {
        NodeContentDTO dto = new NodeContentDTO();
        dto.setId(content.getId());
        dto.setName(content.getName());
        dto.setNote(content.getNote());
        dto.setSystemFrom(content.getSystemFrom().toInstant());
        dto.setSystemTo(content.getSystemTo().toInstant());

        // Metadata MUST be present - data integrity error if null
        NodeMetadata metadata = content.getMetadata();
        Objects.requireNonNull(metadata, "NodeMetadata missing for node: " + content.getId());
        dto.setMetadata(toMetadataDTO(metadata));

        // Recursively map children if depth > 0
        if (depth > 0)
        {
            dto.setChildren(mapChildren(content.getChildren(), depth - 1));
        }

        return dto;
    }

    public static List<NodeContentDTO> toDTOList(NodeContentList nodes, int depth)
    {
        List<NodeContentDTO> result = new ArrayList<>();
        for (NodeContent node : nodes)
        {
            result.add(toDTO(node, depth));
        }
        // Sort by priority
        result.sort(Comparator.comparingInt(dto ->
                dto.getMetadata() != null && dto.getMetadata().getPriority() != null
                        ? dto.getMetadata().getPriority()
                        : 0));
        return result;
    }

    private static List<NodeContentDTO> mapChildren(NodeContentList children, int remainingDepth)
    {
        if (children == null || children.isEmpty())
        {
            return new ArrayList<>();
        }
        return toDTOList(children, remainingDepth);
    }

    private static NodeMetadataDTO toMetadataDTO(NodeMetadata metadata)
    {
        NodeMetadataDTO dto = new NodeMetadataDTO();
        dto.setPriority(metadata.getPriority());
        dto.setCompleted(metadata.isCompleted());
        dto.setCompletedAt(metadata.getCompletedAt() != null ? metadata.getCompletedAt().toInstant() : null);
        dto.setCollapsed(metadata.isCollapsed());
        dto.setLastModified(metadata.getLastModified() != null ? metadata.getLastModified().toInstant() : null);
        dto.setLayoutMode(metadata.getLayoutMode());
        // Handle nullable boolean fields - check isXxxNull() before calling isXxx()
        dto.setVirtualRoot(metadata.isVirtualRootNull() ? null : metadata.isVirtualRoot());
        dto.setReferencesRoot(metadata.isReferencesRootNull() ? null : metadata.isReferencesRoot());
        dto.setInChat(metadata.isInChatNull() ? null : metadata.isInChat());
        dto.setMirrorRoot(metadata.isMirrorRootNull() ? null : metadata.isMirrorRoot());
        dto.setOriginalId(metadata.getOriginalId());
        dto.setChanges(metadata.getChanges());
        dto.setCreatedOn(metadata.getCreatedOn() != null ? metadata.getCreatedOn().toInstant() : null);
        dto.setSystemFrom(metadata.getSystemFrom().toInstant());
        dto.setSystemTo(metadata.getSystemTo().toInstant());
        return dto;
    }

    public static void applyDeepFetch(NodeContentList nodes, int depth)
    {
        // Always deep fetch metadata
        nodes.deepFetch(NodeContentFinder.metadata());

        if (depth >= 1)
        {
            nodes.deepFetch(NodeContentFinder.children());
            nodes.deepFetch(NodeContentFinder.children().metadata());
        }
        if (depth >= 2)
        {
            nodes.deepFetch(NodeContentFinder.children().children());
            nodes.deepFetch(NodeContentFinder.children().children().metadata());
        }
        if (depth >= 3)
        {
            nodes.deepFetch(NodeContentFinder.children().children().children());
            nodes.deepFetch(NodeContentFinder.children().children().children().metadata());
        }
        if (depth >= 4)
        {
            nodes.deepFetch(NodeContentFinder.children().children().children().children());
            nodes.deepFetch(NodeContentFinder.children().children().children().children().metadata());
        }
        if (depth >= 5)
        {
            nodes.deepFetch(NodeContentFinder.children().children().children().children().children());
            nodes.deepFetch(NodeContentFinder.children().children().children().children().children().metadata());
        }
        // Continue for deeper levels as needed (up to 10)
        if (depth >= 6)
        {
            nodes.deepFetch(NodeContentFinder.children().children().children().children().children().children());
            nodes.deepFetch(NodeContentFinder.children().children().children().children().children().children().metadata());
        }
        if (depth >= 7)
        {
            nodes.deepFetch(NodeContentFinder.children().children().children().children().children().children().children());
            nodes.deepFetch(NodeContentFinder.children().children().children().children().children().children().children().metadata());
        }
        if (depth >= 8)
        {
            nodes.deepFetch(NodeContentFinder.children().children().children().children().children().children().children().children());
            nodes.deepFetch(NodeContentFinder.children().children().children().children().children().children().children().children().metadata());
        }
        if (depth >= 9)
        {
            nodes.deepFetch(NodeContentFinder.children().children().children().children().children().children().children().children().children());
            nodes.deepFetch(NodeContentFinder.children().children().children().children().children().children().children().children().children().metadata());
        }
        if (depth >= 10)
        {
            nodes.deepFetch(NodeContentFinder.children().children().children().children().children().children().children().children().children().children());
            nodes.deepFetch(NodeContentFinder.children().children().children().children().children().children().children().children().children().children().metadata());
        }
    }
}
