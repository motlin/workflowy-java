package com.workflowy.data.converter;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WorkflowyFileUtils
{
    private static final Pattern FILE_DATE_PATTERN = Pattern.compile("\\.(\\d{4}-\\d{2}-\\d{2})\\.");
    private static final Pattern FILE_EMAIL_PATTERN = Pattern.compile("^\\((.+?)\\)\\.");

    private WorkflowyFileUtils()
    {
    }

    public static String extractUserIdFromFilename(String filename)
    {
        Matcher matcher = FILE_EMAIL_PATTERN.matcher(filename);
        if (matcher.find())
        {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Could not extract email from filename: " + filename);
    }

    public static String extractUserIdFromFile(File file)
    {
        return extractUserIdFromFilename(file.getName());
    }

    public static Instant getFileTimestamp(String filename)
    {
        Matcher matcher = FILE_DATE_PATTERN.matcher(filename);
        if (matcher.find())
        {
            LocalDate date = LocalDate.parse(matcher.group(1), DateTimeFormatter.ISO_LOCAL_DATE);
            return date.atStartOfDay().toInstant(ZoneOffset.UTC);
        }
        return Instant.MIN;
    }

    public static Instant getFileTimestamp(File file)
    {
        return getFileTimestamp(file.getName());
    }

    public static boolean isAfterHighWatermark(File file, Instant highWatermark)
    {
        Instant fileTimestamp = getFileTimestamp(file);
        return fileTimestamp.isAfter(highWatermark);
    }

    public static boolean isAfterHighWatermark(String filename, Instant highWatermark)
    {
        Instant fileTimestamp = getFileTimestamp(filename);
        return fileTimestamp.isAfter(highWatermark);
    }
}
