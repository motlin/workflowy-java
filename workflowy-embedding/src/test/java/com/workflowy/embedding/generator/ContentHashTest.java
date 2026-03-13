package com.workflowy.embedding.generator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ContentHashTest {

	@Test
	void hashContent_returnsSha256Hex() {
		String hash = EmbeddingGenerator.hashContent("hello world");
		assertNotNull(hash);
		assertEquals(64, hash.length());
	}

	@Test
	void hashContent_sameInputSameHash() {
		String hash1 = EmbeddingGenerator.hashContent("test content");
		String hash2 = EmbeddingGenerator.hashContent("test content");
		assertEquals(hash1, hash2);
	}

	@Test
	void hashContent_differentInputDifferentHash() {
		String hash1 = EmbeddingGenerator.hashContent("content A");
		String hash2 = EmbeddingGenerator.hashContent("content B");
		assertNotEquals(hash1, hash2);
	}

	@Test
	void hashContent_emptyString() {
		String hash = EmbeddingGenerator.hashContent("");
		assertNotNull(hash);
		assertEquals(64, hash.length());
	}

	@Test
	void hashContent_knownSha256() {
		// SHA-256 of "hello" is well-known
		String hash = EmbeddingGenerator.hashContent("hello");
		assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", hash);
	}
}
