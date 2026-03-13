package com.workflowy.embedding.search;

public enum SearchMode {
	VECTOR,
	KEYWORD,
	HYBRID;

	public static SearchMode fromString(String mode) {
		return switch (mode.toLowerCase()) {
			case "vector" -> VECTOR;
			case "keyword" -> KEYWORD;
			case "hybrid" -> HYBRID;
			default -> throw new IllegalArgumentException("Unknown search mode: " + mode + ". Use: vector, keyword, hybrid");
		};
	}
}
