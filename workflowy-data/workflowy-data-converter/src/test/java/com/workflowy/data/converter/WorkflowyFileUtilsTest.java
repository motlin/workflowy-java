package com.workflowy.data.converter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowyFileUtilsTest
{
    @Test
    void extractUserIdFromFilename_withValidFilename_extractsEmail()
    {
        String filename = "(user@example.com).2024-01-15.workflowy.backup";
        assertEquals("user@example.com", WorkflowyFileUtils.extractUserIdFromFilename(filename));
    }

    @Test
    void extractUserIdFromFilename_withComplexEmail_extractsEmail()
    {
        String filename = "(john.doe+test@subdomain.example.co.uk).2024-01-15.workflowy.backup";
        assertEquals("john.doe+test@subdomain.example.co.uk", WorkflowyFileUtils.extractUserIdFromFilename(filename));
    }

    @Test
    void extractUserIdFromFilename_withInvalidFilename_throwsException()
    {
        String filename = "invalid-filename.backup";
        assertThrows(IllegalArgumentException.class,
                () -> WorkflowyFileUtils.extractUserIdFromFilename(filename));
    }

    @Test
    void extractUserIdFromFilename_withMissingParentheses_throwsException()
    {
        String filename = "user@example.com.2024-01-15.workflowy.backup";
        assertThrows(IllegalArgumentException.class,
                () -> WorkflowyFileUtils.extractUserIdFromFilename(filename));
    }

    @Test
    void getFileTimestamp_withValidDatePattern_returnsInstant()
    {
        String filename = "(user@example.com).2024-03-15.workflowy.backup";
        Instant expected = LocalDate.of(2024, 3, 15)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        assertEquals(expected, WorkflowyFileUtils.getFileTimestamp(filename));
    }

    @Test
    void getFileTimestamp_withNoDate_returnsMinInstant()
    {
        String filename = "(user@example.com).workflowy.backup";
        assertEquals(Instant.MIN, WorkflowyFileUtils.getFileTimestamp(filename));
    }

    @Test
    void getFileTimestamp_withLeapYearDate_returnsCorrectInstant()
    {
        String filename = "(user@example.com).2024-02-29.workflowy.backup";
        Instant expected = LocalDate.of(2024, 2, 29)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        assertEquals(expected, WorkflowyFileUtils.getFileTimestamp(filename));
    }

    @Test
    void isAfterHighWatermark_withNewerFile_returnsTrue()
    {
        String filename = "(user@example.com).2024-03-15.workflowy.backup";
        Instant highWatermark = LocalDate.of(2024, 3, 10)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        assertTrue(WorkflowyFileUtils.isAfterHighWatermark(filename, highWatermark));
    }

    @Test
    void isAfterHighWatermark_withOlderFile_returnsFalse()
    {
        String filename = "(user@example.com).2024-03-05.workflowy.backup";
        Instant highWatermark = LocalDate.of(2024, 3, 10)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        assertFalse(WorkflowyFileUtils.isAfterHighWatermark(filename, highWatermark));
    }

    @Test
    void isAfterHighWatermark_withSameDayFile_returnsFalse()
    {
        String filename = "(user@example.com).2024-03-10.workflowy.backup";
        Instant highWatermark = LocalDate.of(2024, 3, 10)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        assertFalse(WorkflowyFileUtils.isAfterHighWatermark(filename, highWatermark));
    }

    @Test
    void isAfterHighWatermark_withMinHighWatermark_returnsTrue()
    {
        String filename = "(user@example.com).2024-03-15.workflowy.backup";
        assertTrue(WorkflowyFileUtils.isAfterHighWatermark(filename, Instant.MIN));
    }

    @Test
    void isAfterHighWatermark_withNoDateAndMinHighWatermark_returnsFalse()
    {
        String filename = "(user@example.com).workflowy.backup";
        assertFalse(WorkflowyFileUtils.isAfterHighWatermark(filename, Instant.MIN));
    }
}
