package com.workflowy.data.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts hashtags from Workflowy item names.
 * Handles both #hashtag and @mention patterns.
 */
public final class HashtagExtractor
{
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("[#@]([a-zA-Z0-9_-]+)");

    private HashtagExtractor()
    {
    }

    /**
     * Extract all hashtags from the given text.
     *
     * @param text The text to search for hashtags
     * @return List of hashtag names (without the # or @ prefix)
     */
    public static List<String> extractHashtags(String text)
    {
        List<String> hashtags = new ArrayList<>();

        if (text == null || text.isEmpty())
        {
            return hashtags;
        }

        String plainText = stripHtmlTags(text);

        Matcher matcher = HASHTAG_PATTERN.matcher(plainText);
        while (matcher.find())
        {
            String tagName = matcher.group(1).toLowerCase();
            if (!hashtags.contains(tagName))
            {
                hashtags.add(tagName);
            }
        }

        return hashtags;
    }

    private static String stripHtmlTags(String html)
    {
        return html.replaceAll("<[^>]*>", "");
    }
}
