package com.workflowy.data.converter;

import java.sql.Timestamp;
import java.time.Instant;

public final class WorkflowyTimestampConverter {

	public static final long WORKFLOWY_EPOCH_OFFSET = 1262304000L;

	private WorkflowyTimestampConverter() {
		throw new AssertionError("Suppress default constructor for noninstantiability");
	}

	public static Timestamp convertWorkflowyTimestamp(Long workflowyTimestamp) {
		if (workflowyTimestamp == null) {
			return null;
		}
		long epochSeconds = workflowyTimestamp + WORKFLOWY_EPOCH_OFFSET;
		return Timestamp.from(Instant.ofEpochSecond(epochSeconds));
	}

	/**
	 * Convert a Unix timestamp (seconds since Jan 1, 1970) to a Timestamp.
	 * Used for API export data which uses standard Unix timestamps.
	 *
	 * @param unixTimestamp Seconds since Unix epoch, or null
	 * @return Corresponding Timestamp, or null if input is null
	 */
	public static Timestamp convertUnixTimestamp(Long unixTimestamp) {
		if (unixTimestamp == null) {
			return null;
		}
		return Timestamp.from(Instant.ofEpochSecond(unixTimestamp));
	}

	/**
	 * Convert a Unix timestamp (seconds since Jan 1, 1970) to an Instant.
	 * Used for API export data which uses standard Unix timestamps.
	 *
	 * @param unixTimestamp Seconds since Unix epoch, or null
	 * @return Corresponding Instant, or null if input is null
	 */
	public static Instant unixTimestampToInstant(Long unixTimestamp) {
		if (unixTimestamp == null) {
			return null;
		}
		return Instant.ofEpochSecond(unixTimestamp);
	}

	public static Instant workflowyTimestampToInstant(Long workflowyTimestamp) {
		if (workflowyTimestamp == null) {
			return null;
		}
		long epochSeconds = workflowyTimestamp + WORKFLOWY_EPOCH_OFFSET;
		return Instant.ofEpochSecond(epochSeconds);
	}
}
