package com.workflowy.data.converter;

import java.sql.Timestamp;
import java.time.Instant;

public final class WorkflowyTimestampConverter
{
    public static final long WORKFLOWY_EPOCH_OFFSET = 1262304000L;

    private WorkflowyTimestampConverter()
    {
    }

    public static Timestamp convertWorkflowyTimestamp(Long workflowyTimestamp)
    {
        if (workflowyTimestamp == null)
        {
            return null;
        }
        long epochSeconds = workflowyTimestamp + WORKFLOWY_EPOCH_OFFSET;
        return Timestamp.from(Instant.ofEpochSecond(epochSeconds));
    }

    public static Timestamp parseCalendarDate(Object dateValue)
    {
        if (dateValue instanceof Number number)
        {
            long epochSeconds = number.longValue();
            return Timestamp.from(Instant.ofEpochSecond(epochSeconds));
        }
        return null;
    }

    public static Instant workflowyTimestampToInstant(Long workflowyTimestamp)
    {
        if (workflowyTimestamp == null)
        {
            return null;
        }
        long epochSeconds = workflowyTimestamp + WORKFLOWY_EPOCH_OFFSET;
        return Instant.ofEpochSecond(epochSeconds);
    }
}
