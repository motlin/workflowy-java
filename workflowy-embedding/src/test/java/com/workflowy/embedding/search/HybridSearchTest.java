package com.workflowy.embedding.search;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HybridSearchTest {

	@Test
	void fuseResults_combinesVectorAndKeywordResults() {
		List<SearchResult> vectorResults = List.of(
			new SearchResult("node-a", 0.1),
			new SearchResult("node-b", 0.2),
			new SearchResult("node-c", 0.3)
		);

		List<SearchResult> keywordResults = List.of(
			new SearchResult("node-b", 0.15),
			new SearchResult("node-d", 0.25),
			new SearchResult("node-a", 0.35)
		);

		List<SearchResult> fused = SearchEngine.fuseResults(vectorResults, keywordResults, 10);

		assertEquals(4, fused.size());

		// node-a and node-b appear in both lists, so they should have higher RRF scores
		// node-a: vector rank 0 → 1/(60+0+1) = 1/61, keyword rank 2 → 1/(60+2+1) = 1/63
		// node-b: vector rank 1 → 1/(60+1+1) = 1/62, keyword rank 0 → 1/(60+0+1) = 1/61
		// node-b should rank first (1/62 + 1/61 > 1/61 + 1/63)
		assertEquals("node-b", fused.getFirst().getNodeId());
		assertEquals("node-a", fused.get(1).getNodeId());
	}

	@Test
	void fuseResults_respectsLimit() {
		List<SearchResult> vectorResults = List.of(
			new SearchResult("node-a", 0.1),
			new SearchResult("node-b", 0.2),
			new SearchResult("node-c", 0.3)
		);

		List<SearchResult> keywordResults = List.of(new SearchResult("node-d", 0.1), new SearchResult("node-e", 0.2));

		List<SearchResult> fused = SearchEngine.fuseResults(vectorResults, keywordResults, 2);
		assertEquals(2, fused.size());
	}

	@Test
	void fuseResults_distanceIsOneMinusRrfScore() {
		List<SearchResult> vectorResults = List.of(new SearchResult("node-a", 0.1));
		List<SearchResult> keywordResults = List.of();

		List<SearchResult> fused = SearchEngine.fuseResults(vectorResults, keywordResults, 10);

		assertEquals(1, fused.size());
		// node-a: vector rank 0 → RRF score = 1/(60+0+1) = 1/61
		// distance = 1.0 - 1/61
		double expectedDistance = 1.0 - (1.0 / 61.0);
		assertEquals(expectedDistance, fused.getFirst().getDistance(), 0.0001);
	}

	@Test
	void fuseResults_withBothEmpty_returnsEmpty() {
		List<SearchResult> fused = SearchEngine.fuseResults(List.of(), List.of(), 10);
		assertTrue(fused.isEmpty());
	}

	@Test
	void fuseResults_overlappingResults_rankedHigher() {
		// A node appearing in both result sets should always rank higher
		// than a node appearing in only one (assuming similar positions)
		List<SearchResult> vectorResults = List.of(new SearchResult("only-vector", 0.1), new SearchResult("both", 0.2));

		List<SearchResult> keywordResults = List.of(
			new SearchResult("only-keyword", 0.1),
			new SearchResult("both", 0.2)
		);

		List<SearchResult> fused = SearchEngine.fuseResults(vectorResults, keywordResults, 10);

		assertEquals("both", fused.getFirst().getNodeId());
	}

	@Test
	void fuseResults_sortedByDescendingRrfScore() {
		List<SearchResult> vectorResults = List.of(
			new SearchResult("a", 0.1),
			new SearchResult("b", 0.2),
			new SearchResult("c", 0.3)
		);

		List<SearchResult> keywordResults = List.of(
			new SearchResult("c", 0.1),
			new SearchResult("b", 0.2),
			new SearchResult("a", 0.3)
		);

		List<SearchResult> fused = SearchEngine.fuseResults(vectorResults, keywordResults, 10);

		// All three appear in both lists with symmetric positions
		// a: 1/61 + 1/63, b: 1/62 + 1/62, c: 1/63 + 1/61
		// a and c have equal scores (symmetric), b has score 2/62 = 1/31
		// 1/61 + 1/63 = (63+61)/(61*63) = 124/3843 ≈ 0.03227
		// 2/62 = 1/31 ≈ 0.03226
		// All very close — verify they're at least sorted by distance ascending
		for (var i = 0; i < fused.size() - 1; i++) {
			assertTrue(fused.get(i).getDistance() <= fused.get(i + 1).getDistance());
		}
	}
}
