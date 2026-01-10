package com.workflowy.dropwizard.application.cli;

import java.io.IOException;

import org.fusesource.jansi.Ansi;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSyntaxHighlighterTest
{
    @BeforeAll
    static void setUp()
    {
        Ansi.setEnabled(true);
    }

    @Test
    void highlight_withEmptyObject_highlightsBraces() throws IOException
    {
        String result = JsonSyntaxHighlighter.highlight("{}");
        assertTrue(result.contains("{"));
        assertTrue(result.contains("}"));
    }

    @Test
    void highlight_withEmptyArray_highlightsBrackets() throws IOException
    {
        String result = JsonSyntaxHighlighter.highlight("[]");
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
    }

    @Test
    void highlight_withStringValue_containsAnsiCodes() throws IOException
    {
        String result = JsonSyntaxHighlighter.highlight("{\"name\": \"value\"}");
        // Check that ANSI escape codes are present (ESC = \u001B)
        assertTrue(result.contains("\u001B["));
        assertTrue(result.contains("name"));
        assertTrue(result.contains("value"));
    }

    @Test
    void highlight_withNumberValue_preservesNumber() throws IOException
    {
        String result = JsonSyntaxHighlighter.highlight("{\"count\": 42}");
        assertTrue(result.contains("42"));
    }

    @Test
    void highlight_withFloatValue_preservesFloat() throws IOException
    {
        String result = JsonSyntaxHighlighter.highlight("{\"price\": 19.99}");
        assertTrue(result.contains("19.99"));
    }

    @Test
    void highlight_withBooleanTrue_preservesTrue() throws IOException
    {
        String result = JsonSyntaxHighlighter.highlight("{\"active\": true}");
        assertTrue(result.contains("true"));
    }

    @Test
    void highlight_withBooleanFalse_preservesFalse() throws IOException
    {
        String result = JsonSyntaxHighlighter.highlight("{\"active\": false}");
        assertTrue(result.contains("false"));
    }

    @Test
    void highlight_withNullValue_preservesNull() throws IOException
    {
        String result = JsonSyntaxHighlighter.highlight("{\"value\": null}");
        assertTrue(result.contains("null"));
    }

    @Test
    void highlight_withNestedObject_highlightsAll() throws IOException
    {
        String result = JsonSyntaxHighlighter.highlight("{\"outer\": {\"inner\": \"value\"}}");
        assertTrue(result.contains("outer"));
        assertTrue(result.contains("inner"));
        assertTrue(result.contains("value"));
    }

    @Test
    void highlight_withArray_highlightsElements() throws IOException
    {
        String result = JsonSyntaxHighlighter.highlight("{\"items\": [1, 2, 3]}");
        assertTrue(result.contains("items"));
        assertTrue(result.contains("1"));
        assertTrue(result.contains("2"));
        assertTrue(result.contains("3"));
    }

    @Test
    void highlight_withComplexJson_maintainsStructure() throws IOException
    {
        String input = """
                {
                  "name": "Test",
                  "count": 5,
                  "enabled": true,
                  "data": null,
                  "items": ["a", "b"]
                }""";
        String result = JsonSyntaxHighlighter.highlight(input);

        assertTrue(result.contains("name"));
        assertTrue(result.contains("Test"));
        assertTrue(result.contains("count"));
        assertTrue(result.contains("5"));
        assertTrue(result.contains("enabled"));
        assertTrue(result.contains("true"));
        assertTrue(result.contains("data"));
        assertTrue(result.contains("null"));
        assertTrue(result.contains("items"));
    }

    @Test
    void highlight_withInvalidJson_throwsIOException()
    {
        assertThrows(IOException.class, () -> JsonSyntaxHighlighter.highlight("{invalid}"));
    }

    @Test
    void highlight_withEmptyString_returnsEmpty() throws IOException
    {
        String result = JsonSyntaxHighlighter.highlight("");
        assertEquals("", result);
    }

    @Test
    void highlight_preservesWhitespaceStructure() throws IOException
    {
        String input = "{\n  \"key\": \"value\"\n}";
        String result = JsonSyntaxHighlighter.highlight(input);
        // Check that the structure with newlines is preserved
        assertTrue(result.contains("\n"));
    }

    @Test
    void highlight_withEscapedStrings_preservesContent() throws IOException
    {
        String result = JsonSyntaxHighlighter.highlight("{\"text\": \"line1\\nline2\"}");
        assertTrue(result.contains("text"));
    }
}
