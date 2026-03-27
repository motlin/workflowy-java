package com.workflowy.data.converter;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class WorkflowyTimestampConverterTest {

	@Test
	void workflowyEpochOffset_isJanuary1st2010() {
		Instant epoch = Instant.ofEpochSecond(WorkflowyTimestampConverter.WORKFLOWY_EPOCH_OFFSET);
		assertEquals(Instant.parse("2010-01-01T00:00:00Z"), epoch);
	}

	@Test
	void convertWorkflowyTimestamp_withNull_returnsNull() {
		assertNull(WorkflowyTimestampConverter.convertWorkflowyTimestamp(null));
	}

	@Test
	void convertWorkflowyTimestamp_withZero_returnsWorkflowyEpoch() {
		Timestamp result = WorkflowyTimestampConverter.convertWorkflowyTimestamp(0L);
		assertNotNull(result);
		assertEquals(Instant.parse("2010-01-01T00:00:00Z"), result.toInstant());
	}

	@Test
	void convertWorkflowyTimestamp_withPositiveValue_returnsCorrectTimestamp() {
		long workflowyTimestamp = 86400L;
		Timestamp result = WorkflowyTimestampConverter.convertWorkflowyTimestamp(workflowyTimestamp);
		assertNotNull(result);
		assertEquals(Instant.parse("2010-01-02T00:00:00Z"), result.toInstant());
	}

	@Test
	void convertWorkflowyTimestamp_withRecentDate_returnsCorrectTimestamp() {
		long secondsSinceWorkflowyEpoch =
			Instant.parse("2024-01-15T12:00:00Z").getEpochSecond() - WorkflowyTimestampConverter.WORKFLOWY_EPOCH_OFFSET;
		Timestamp result = WorkflowyTimestampConverter.convertWorkflowyTimestamp(secondsSinceWorkflowyEpoch);
		assertNotNull(result);
		assertEquals(Instant.parse("2024-01-15T12:00:00Z"), result.toInstant());
	}

	@Test
	void workflowyTimestampToInstant_withNull_returnsNull() {
		assertNull(WorkflowyTimestampConverter.workflowyTimestampToInstant(null));
	}

	@Test
	void workflowyTimestampToInstant_withZero_returnsWorkflowyEpoch() {
		Instant result = WorkflowyTimestampConverter.workflowyTimestampToInstant(0L);
		assertNotNull(result);
		assertEquals(Instant.parse("2010-01-01T00:00:00Z"), result);
	}
}
