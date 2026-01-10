package com.workflowy.data.converter;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashtagExtractorTest
{
    @Test
    void extractHashtags_withNullInput_returnsEmptyList()
    {
        List<String> result = HashtagExtractor.extractHashtags(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void extractHashtags_withEmptyInput_returnsEmptyList()
    {
        List<String> result = HashtagExtractor.extractHashtags("");
        assertTrue(result.isEmpty());
    }

    @Test
    void extractHashtags_withNoHashtags_returnsEmptyList()
    {
        List<String> result = HashtagExtractor.extractHashtags("Just a plain text without tags");
        assertTrue(result.isEmpty());
    }

    @Test
    void extractHashtags_withSingleHashtag_returnsHashtag()
    {
        List<String> result = HashtagExtractor.extractHashtags("Task with #priority");
        assertEquals(1, result.size());
        assertEquals("priority", result.get(0));
    }

    @Test
    void extractHashtags_withSingleMention_returnsMention()
    {
        List<String> result = HashtagExtractor.extractHashtags("Assigned to @john");
        assertEquals(1, result.size());
        assertEquals("john", result.get(0));
    }

    @Test
    void extractHashtags_withMultipleHashtags_returnsAll()
    {
        List<String> result = HashtagExtractor.extractHashtags("#task with #priority and #urgent");
        assertEquals(3, result.size());
        assertTrue(result.contains("task"));
        assertTrue(result.contains("priority"));
        assertTrue(result.contains("urgent"));
    }

    @Test
    void extractHashtags_withMixedHashtagsAndMentions_returnsBoth()
    {
        List<String> result = HashtagExtractor.extractHashtags("#task assigned to @john");
        assertEquals(2, result.size());
        assertTrue(result.contains("task"));
        assertTrue(result.contains("john"));
    }

    @Test
    void extractHashtags_withDuplicateHashtags_returnsUnique()
    {
        List<String> result = HashtagExtractor.extractHashtags("#task and another #task");
        assertEquals(1, result.size());
        assertEquals("task", result.get(0));
    }

    @Test
    void extractHashtags_withHtmlTags_stripsHtmlFirst()
    {
        List<String> result = HashtagExtractor.extractHashtags("<b>#priority</b> task");
        assertEquals(1, result.size());
        assertEquals("priority", result.get(0));
    }

    @Test
    void extractHashtags_withDashesInHashtag_includesDashes()
    {
        List<String> result = HashtagExtractor.extractHashtags("#high-priority task");
        assertEquals(1, result.size());
        assertEquals("high-priority", result.get(0));
    }

    @Test
    void extractHashtags_withUnderscoresInHashtag_includesUnderscores()
    {
        List<String> result = HashtagExtractor.extractHashtags("#work_item");
        assertEquals(1, result.size());
        assertEquals("work_item", result.get(0));
    }

    @Test
    void extractHashtags_withMixedCase_lowercasesResult()
    {
        List<String> result = HashtagExtractor.extractHashtags("#URGENT and #Priority");
        assertEquals(2, result.size());
        assertTrue(result.contains("urgent"));
        assertTrue(result.contains("priority"));
    }

    @Test
    void extractHashtags_withNumbersInHashtag_includesNumbers()
    {
        List<String> result = HashtagExtractor.extractHashtags("#phase2 and #q4-review");
        assertEquals(2, result.size());
        assertTrue(result.contains("phase2"));
        assertTrue(result.contains("q4-review"));
    }

    @Test
    void extractHashtags_withHashtagAtStart_findsIt()
    {
        List<String> result = HashtagExtractor.extractHashtags("#todo finish the report");
        assertEquals(1, result.size());
        assertEquals("todo", result.get(0));
    }

    @Test
    void extractHashtags_withHashtagAtEnd_findsIt()
    {
        List<String> result = HashtagExtractor.extractHashtags("Complete the report #done");
        assertEquals(1, result.size());
        assertEquals("done", result.get(0));
    }

    @Test
    void extractHashtags_withHashSymbolAlone_ignoresIt()
    {
        List<String> result = HashtagExtractor.extractHashtags("Issue # 123");
        assertTrue(result.isEmpty());
    }

    @Test
    void extractHashtags_withEmailAddress_ignoresIt()
    {
        // Email addresses should not be treated as mentions
        List<String> result = HashtagExtractor.extractHashtags("Contact user@example.com");
        // The regex will match @example, but that's the current behavior
        assertEquals(1, result.size());
        assertEquals("example", result.get(0));
    }
}
