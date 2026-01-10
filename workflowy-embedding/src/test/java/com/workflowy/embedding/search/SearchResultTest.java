package com.workflowy.embedding.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchResultTest
{
    @Test
    void constructor_setsNodeIdAndDistance()
    {
        SearchResult result = new SearchResult("node-123", 0.3);
        assertEquals("node-123", result.getNodeId());
        assertEquals(0.3, result.getDistance());
    }

    @Test
    void getSimilarity_isOneMinusDistance()
    {
        SearchResult result = new SearchResult("node-123", 0.3);
        assertEquals(0.7, result.getSimilarity(), 0.0001);
    }

    @Test
    void getSimilarity_withZeroDistance_returnsOne()
    {
        SearchResult result = new SearchResult("node-123", 0.0);
        assertEquals(1.0, result.getSimilarity());
    }

    @Test
    void getSimilarity_withOneDistance_returnsZero()
    {
        SearchResult result = new SearchResult("node-123", 1.0);
        assertEquals(0.0, result.getSimilarity());
    }

    @Test
    void enrichmentFields_areNullByDefault()
    {
        SearchResult result = new SearchResult("node-123", 0.3);
        assertNull(result.getName());
        assertNull(result.getNote());
        assertNull(result.getFullPath());
        assertNull(result.getTextContent());
    }

    @Test
    void setName_updatesName()
    {
        SearchResult result = new SearchResult("node-123", 0.3);
        result.setName("Test Node");
        assertEquals("Test Node", result.getName());
    }

    @Test
    void setNote_updatesNote()
    {
        SearchResult result = new SearchResult("node-123", 0.3);
        result.setNote("This is a note");
        assertEquals("This is a note", result.getNote());
    }

    @Test
    void setFullPath_updatesFullPath()
    {
        SearchResult result = new SearchResult("node-123", 0.3);
        result.setFullPath("Root > Parent > Child");
        assertEquals("Root > Parent > Child", result.getFullPath());
    }

    @Test
    void setTextContent_updatesTextContent()
    {
        SearchResult result = new SearchResult("node-123", 0.3);
        result.setTextContent("Combined text content");
        assertEquals("Combined text content", result.getTextContent());
    }

    @Test
    void toString_includesKeyFields()
    {
        SearchResult result = new SearchResult("node-123", 0.3);
        result.setName("Test Node");
        result.setFullPath("Root > Parent > Child");

        String str = result.toString();
        assertTrue(str.contains("node-123"));
        assertTrue(str.contains("0.3"));
        assertTrue(str.contains("Test Node"));
        assertTrue(str.contains("Root > Parent > Child"));
    }

    @Test
    void toString_handlesNullValues()
    {
        SearchResult result = new SearchResult("node-123", 0.3);
        String str = result.toString();
        assertTrue(str.contains("node-123"));
        assertTrue(str.contains("null"));
    }
}
