package com.workflowy.embedding.util;

import java.util.regex.Pattern;

public final class HtmlStripper
{
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private HtmlStripper()
    {
    }

    public static String stripHtmlTags(String html)
    {
        if (html == null || html.isEmpty())
        {
            return "";
        }

        String withoutTags = HTML_TAG_PATTERN.matcher(html).replaceAll("");

        withoutTags = withoutTags
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'");

        withoutTags = WHITESPACE_PATTERN.matcher(withoutTags).replaceAll(" ");

        return withoutTags.trim();
    }
}
