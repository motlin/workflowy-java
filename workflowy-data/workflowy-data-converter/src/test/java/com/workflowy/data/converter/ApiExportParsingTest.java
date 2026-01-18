package com.workflowy.data.converter;

import java.io.File;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflowy.data.pojo.ApiInputItem;
import com.workflowy.data.pojo.ApiResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiExportParsingTest {

	private static final String API_EXPORT_PATH = "/Users/craig/projects/workflowy/backups/api-export.json";

	@Test
	void parseApiExportJson() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

		String json = """
			{
			  "nodes": [
			    {
			      "id": "a5408f02-03aa-4735-a886-4ccd128d1ad7",
			      "name": "Root Node",
			      "note": "A note with details",
			      "parent_id": null,
			      "priority": 0,
			      "completed": false,
			      "createdAt": 1704067200,
			      "modifiedAt": 1704153600,
			      "completedAt": null,
			      "data": {
			        "layoutMode": "board",
			        "ai": {"inChat": true},
			        "isReferencesRoot": false
			      }
			    },
			    {
			      "id": "b6509f13-14bb-5846-b997-5dde239e2be8",
			      "name": "Child Node #tag",
			      "note": null,
			      "parent_id": "a5408f02-03aa-4735-a886-4ccd128d1ad7",
			      "priority": 1,
			      "completed": true,
			      "createdAt": 1704067200,
			      "modifiedAt": 1704240000,
			      "completedAt": 1704240000
			    }
			  ]
			}
			""";

		ApiResponse response = objectMapper.readValue(json, ApiResponse.class);
		List<ApiInputItem> nodes = response.nodes();

		assertEquals(2, nodes.size());

		ApiInputItem root = nodes.get(0);
		assertEquals("a5408f02-03aa-4735-a886-4ccd128d1ad7", root.id());
		assertEquals("Root Node", root.name());
		assertEquals("A note with details", root.note());
		assertNull(root.parentId());
		assertEquals(0, root.priority());
		assertFalse(root.completed());
		assertEquals(1704067200L, root.createdAt());
		assertEquals(1704153600L, root.modifiedAt());
		assertNull(root.completedAt());
		assertEquals("board", root.layoutMode());
		assertTrue(root.isInChat());
		assertFalse(root.isReferencesRoot());

		ApiInputItem child = nodes.get(1);
		assertEquals("b6509f13-14bb-5846-b997-5dde239e2be8", child.id());
		assertEquals("Child Node #tag", child.name());
		assertNull(child.note());
		assertEquals("a5408f02-03aa-4735-a886-4ccd128d1ad7", child.parentId());
		assertEquals(1, child.priority());
		assertTrue(child.completed());
		assertEquals(1704240000L, child.completedAt());
		assertNull(child.layoutMode());
		assertFalse(child.isInChat());
	}

	@Test
	void parseMinimalNode() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

		String json = """
			{
			  "nodes": [
			    {
			      "id": "test-id",
			      "name": "Test",
			      "priority": 0,
			      "completed": false,
			      "createdAt": 1704067200
			    }
			  ]
			}
			""";

		ApiResponse response = objectMapper.readValue(json, ApiResponse.class);
		assertEquals(1, response.nodes().size());

		ApiInputItem node = response.nodes().get(0);
		assertEquals("test-id", node.id());
		assertEquals("Test", node.name());
		assertNull(node.note());
		assertNull(node.parentId());
		assertFalse(node.completed());
	}

	@Test
	@Disabled("Uses real API export file not in VCS")
	void parseRealApiExportFile() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

		File exportFile = new File(API_EXPORT_PATH);
		if (!exportFile.exists()) {
			System.out.println("Skipping test - file not found: " + API_EXPORT_PATH);
			return;
		}

		System.out.println("Parsing: " + exportFile.getName());
		ApiResponse response = objectMapper.readValue(exportFile, ApiResponse.class);
		List<ApiInputItem> nodes = response.nodes();
		System.out.println("  Parsed " + nodes.size() + " nodes");

		long completedCount = nodes.stream().filter(ApiInputItem::completed).count();
		long rootCount = nodes.stream().filter(n -> n.parentId() == null).count();
		System.out.println("  Completed: " + completedCount);
		System.out.println("  Root nodes: " + rootCount);
	}
}
