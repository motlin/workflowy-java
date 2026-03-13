package com.workflowy.embedding.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchModeTest {

	@Test
	void fromString_vector() {
		assertEquals(SearchMode.VECTOR, SearchMode.fromString("vector"));
		assertEquals(SearchMode.VECTOR, SearchMode.fromString("VECTOR"));
		assertEquals(SearchMode.VECTOR, SearchMode.fromString("Vector"));
	}

	@Test
	void fromString_keyword() {
		assertEquals(SearchMode.KEYWORD, SearchMode.fromString("keyword"));
	}

	@Test
	void fromString_hybrid() {
		assertEquals(SearchMode.HYBRID, SearchMode.fromString("hybrid"));
	}

	@Test
	void fromString_unknown_throws() {
		assertThrows(IllegalArgumentException.class, () -> SearchMode.fromString("unknown"));
	}
}
