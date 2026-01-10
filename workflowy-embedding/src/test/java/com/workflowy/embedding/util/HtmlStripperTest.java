package com.workflowy.embedding.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HtmlStripperTest
{
    @Test
    void stripHtmlTags_withNullInput_returnsEmptyString()
    {
        assertEquals("", HtmlStripper.stripHtmlTags(null));
    }

    @Test
    void stripHtmlTags_withEmptyInput_returnsEmptyString()
    {
        assertEquals("", HtmlStripper.stripHtmlTags(""));
    }

    @Test
    void stripHtmlTags_withPlainText_returnsUnchanged()
    {
        assertEquals("Hello World", HtmlStripper.stripHtmlTags("Hello World"));
    }

    @Test
    void stripHtmlTags_withSimpleHtmlTag_stripsTag()
    {
        assertEquals("Hello", HtmlStripper.stripHtmlTags("<b>Hello</b>"));
    }

    @Test
    void stripHtmlTags_withNestedTags_stripsAllTags()
    {
        assertEquals("Hello World", HtmlStripper.stripHtmlTags("<div><b>Hello</b> <i>World</i></div>"));
    }

    @Test
    void stripHtmlTags_withSelfClosingTag_stripsTag()
    {
        assertEquals("HelloWorld", HtmlStripper.stripHtmlTags("Hello<br/>World"));
    }

    @Test
    void stripHtmlTags_withNbspEntity_replacesWithSpace()
    {
        assertEquals("Hello World", HtmlStripper.stripHtmlTags("Hello&nbsp;World"));
    }

    @Test
    void stripHtmlTags_withAmpEntity_replacesWithAmpersand()
    {
        assertEquals("Tom & Jerry", HtmlStripper.stripHtmlTags("Tom &amp; Jerry"));
    }

    @Test
    void stripHtmlTags_withLtEntity_replacesWithLessThan()
    {
        assertEquals("a < b", HtmlStripper.stripHtmlTags("a &lt; b"));
    }

    @Test
    void stripHtmlTags_withGtEntity_replacesWithGreaterThan()
    {
        assertEquals("a > b", HtmlStripper.stripHtmlTags("a &gt; b"));
    }

    @Test
    void stripHtmlTags_withQuotEntity_replacesWithQuote()
    {
        assertEquals("He said \"hello\"", HtmlStripper.stripHtmlTags("He said &quot;hello&quot;"));
    }

    @Test
    void stripHtmlTags_withAposEntity_replacesWithApostrophe()
    {
        assertEquals("it's fine", HtmlStripper.stripHtmlTags("it&apos;s fine"));
    }

    @Test
    void stripHtmlTags_withNumericAposEntity_replacesWithApostrophe()
    {
        assertEquals("it's fine", HtmlStripper.stripHtmlTags("it&#39;s fine"));
    }

    @Test
    void stripHtmlTags_withMultipleWhitespace_normalizesToSingleSpace()
    {
        assertEquals("Hello World", HtmlStripper.stripHtmlTags("Hello    World"));
    }

    @Test
    void stripHtmlTags_withNewlinesAndTabs_normalizesToSingleSpace()
    {
        assertEquals("Hello World", HtmlStripper.stripHtmlTags("Hello\n\t\tWorld"));
    }

    @Test
    void stripHtmlTags_withLeadingWhitespace_trims()
    {
        assertEquals("Hello", HtmlStripper.stripHtmlTags("   Hello"));
    }

    @Test
    void stripHtmlTags_withTrailingWhitespace_trims()
    {
        assertEquals("Hello", HtmlStripper.stripHtmlTags("Hello   "));
    }

    @Test
    void stripHtmlTags_withComplexWorkflowyContent_handlesCorrectly()
    {
        String input = "<b>Task:</b>&nbsp;Complete the&nbsp;<i>project</i>&amp;review";
        String expected = "Task: Complete the project&review";
        assertEquals(expected, HtmlStripper.stripHtmlTags(input));
    }

    @Test
    void stripHtmlTags_withMultipleEntities_replacesAll()
    {
        String input = "&lt;div&gt; &quot;test&quot; &amp; &#39;value&apos;";
        String expected = "<div> \"test\" & 'value'";
        assertEquals(expected, HtmlStripper.stripHtmlTags(input));
    }
}
