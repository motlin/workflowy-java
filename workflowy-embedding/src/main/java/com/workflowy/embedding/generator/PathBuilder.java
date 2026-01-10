package com.workflowy.embedding.generator;

import java.util.ArrayList;
import java.util.List;

import com.workflowy.NodeContent;
import com.workflowy.NodeContentFinder;
import com.workflowy.embedding.util.HtmlStripper;

public class PathBuilder
{
    public String buildFullPath(String nodeId)
    {
        List<String> path = new ArrayList<>();
        String currentId = nodeId;

        while (currentId != null)
        {
            NodeContent node = NodeContentFinder.findOne(NodeContentFinder.id().eq(currentId));

            if (node == null)
            {
                break;
            }

            String name = HtmlStripper.stripHtmlTags(node.getName());
            if (name != null && !name.isEmpty())
            {
                path.add(0, name);
            }

            currentId = node.getParentId();
        }

        return String.join(" > ", path);
    }

    public String buildTextContent(String nodeId)
    {
        NodeContent node = NodeContentFinder.findOne(NodeContentFinder.id().eq(nodeId));

        if (node == null)
        {
            return "";
        }

        String name = HtmlStripper.stripHtmlTags(node.getName());
        String note = node.getNote() != null ? HtmlStripper.stripHtmlTags(node.getNote()) : "";

        if (note.isEmpty())
        {
            return name != null ? name : "";
        }
        else
        {
            return (name != null ? name : "") + "\n\n" + note;
        }
    }

    public String buildEmbeddingText(String nodeId)
    {
        String fullPath = this.buildFullPath(nodeId);
        String textContent = this.buildTextContent(nodeId);

        if (fullPath.isEmpty())
        {
            return textContent;
        }
        else if (textContent.isEmpty())
        {
            return fullPath;
        }
        else
        {
            return fullPath + "\n\n" + textContent;
        }
    }
}
