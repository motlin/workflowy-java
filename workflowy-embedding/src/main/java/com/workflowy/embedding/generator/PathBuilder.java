package com.workflowy.embedding.generator;

import com.workflowy.NodeContent;
import com.workflowy.NodeContentFinder;
import com.workflowy.NodeContentList;
import com.workflowy.embedding.util.HtmlStripper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

public class PathBuilder {

	public String buildFullPath(String nodeId) {
		MutableList<String> path = Lists.mutable.empty();
		String currentId = nodeId;

		while (currentId != null) {
			NodeContent node = NodeContentFinder.findOne(NodeContentFinder.id().eq(currentId));

			if (node == null) {
				break;
			}

			String name = HtmlStripper.stripHtmlTags(node.getName());
			if (name != null && !name.isEmpty()) {
				path.addFirst(name);
			}

			currentId = node.getParentId();
		}

		return String.join(" > ", path);
	}

	public String buildTextContent(String nodeId) {
		NodeContent node = NodeContentFinder.findOne(NodeContentFinder.id().eq(nodeId));

		if (node == null) {
			return "";
		}

		String name = HtmlStripper.stripHtmlTags(node.getName());
		String note = node.getNote() != null ? HtmlStripper.stripHtmlTags(node.getNote()) : "";

		if (note.isEmpty()) {
			return name != null ? name : "";
		} else {
			return (name != null ? name : "") + "\n\n" + note;
		}
	}

	public String buildChildrenText(String nodeId) {
		NodeContentList children = NodeContentFinder.findMany(NodeContentFinder.parentId().eq(nodeId));

		if (children.isEmpty()) {
			return "";
		}

		MutableList<String> childTexts = Lists.mutable.empty();
		for (var i = 0; i < children.size(); i++) {
			NodeContent child = children.get(i);
			String name = HtmlStripper.stripHtmlTags(child.getName());
			String note = child.getNote() != null ? HtmlStripper.stripHtmlTags(child.getNote()) : "";

			if (name == null || name.isEmpty()) {
				continue;
			}

			if (note.isEmpty()) {
				childTexts.add(name);
			} else {
				childTexts.add(name + ": " + note);
			}
		}

		return String.join("\n", childTexts);
	}

	public boolean hasChildren(String nodeId) {
		NodeContentList children = NodeContentFinder.findMany(NodeContentFinder.parentId().eq(nodeId));
		return !children.isEmpty();
	}

	public String buildEmbeddingText(String nodeId) {
		String fullPath = this.buildFullPath(nodeId);
		String textContent = this.buildTextContent(nodeId);
		String childrenText = this.buildChildrenText(nodeId);

		var sb = new StringBuilder();

		if (!fullPath.isEmpty()) {
			sb.append("PATH: ").append(fullPath).append('\n');
		}

		if (!textContent.isEmpty()) {
			sb.append("CONTENT: ").append(textContent);
		}

		if (!childrenText.isEmpty()) {
			sb.append("\nCHILDREN:\n").append(childrenText);
		}

		return sb.toString();
	}
}
