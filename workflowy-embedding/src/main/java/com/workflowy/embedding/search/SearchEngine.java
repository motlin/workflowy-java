package com.workflowy.embedding.search;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.workflowy.NodeContent;
import com.workflowy.NodeContentFinder;
import com.workflowy.embedding.engine.EmbeddingEngine;
import com.workflowy.embedding.generator.PathBuilder;
import com.workflowy.embedding.model.EmbeddingModel;
import com.workflowy.embedding.repository.EmbeddingRepository;
import com.workflowy.embedding.util.HtmlStripper;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchEngine {

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchEngine.class);

	private static final int RRF_K = 60;

	private final EmbeddingEngine engine;
	private final EmbeddingRepository repository;
	private final PathBuilder pathBuilder;

	public SearchEngine(EmbeddingEngine engine, EmbeddingRepository repository) {
		this.engine = engine;
		this.repository = repository;
		this.pathBuilder = new PathBuilder();
	}

	public List<SearchResult> search(String query, int limit, Double threshold) throws SQLException {
		return this.search(query, limit, threshold, SearchMode.VECTOR);
	}

	public List<SearchResult> search(String query, int limit, Double threshold, SearchMode mode) throws SQLException {
		List<SearchResult> results = switch (mode) {
			case VECTOR -> this.searchVector(query, limit, threshold);
			case KEYWORD -> this.searchKeyword(query, limit);
			case HYBRID -> this.searchHybrid(query, limit, threshold);
		};

		for (SearchResult result : results) {
			this.enrichResult(result);
		}

		return results;
	}

	private List<SearchResult> searchVector(String query, int limit, Double threshold) throws SQLException {
		EmbeddingModel model = this.engine.getModel();
		double effectiveThreshold = threshold != null ? threshold : model.getDefaultThreshold();
		float[] queryEmbedding = this.engine.generateEmbedding(query, true);
		return this.repository.search(queryEmbedding, model, limit, effectiveThreshold);
	}

	private List<SearchResult> searchKeyword(String query, int limit) throws SQLException {
		return this.repository.searchKeyword(query, limit);
	}

	List<SearchResult> searchHybrid(String query, int limit, Double threshold) throws SQLException {
		int candidateLimit = limit * 3;

		List<SearchResult> vectorResults = this.searchVector(query, candidateLimit, threshold);
		List<SearchResult> keywordResults = this.searchKeyword(query, candidateLimit);

		return fuseResults(vectorResults, keywordResults, limit);
	}

	static List<SearchResult> fuseResults(
		List<SearchResult> vectorResults,
		List<SearchResult> keywordResults,
		int limit
	) {
		MutableMap<String, Double> rrfScores = Maps.mutable.empty();

		for (int rank = 0; rank < vectorResults.size(); rank++) {
			String nodeId = vectorResults.get(rank).getNodeId();
			double score = 1.0 / (RRF_K + rank + 1);
			rrfScores.merge(nodeId, score, Double::sum);
		}

		for (int rank = 0; rank < keywordResults.size(); rank++) {
			String nodeId = keywordResults.get(rank).getNodeId();
			double score = 1.0 / (RRF_K + rank + 1);
			rrfScores.merge(nodeId, score, Double::sum);
		}

		return rrfScores.entrySet()
			.stream()
			.sorted(Map.Entry.<String, Double>comparingByValue().reversed())
			.limit(limit)
			.map(entry -> new SearchResult(entry.getKey(), 1.0 - entry.getValue()))
			.toList();
	}

	private void enrichResult(SearchResult result) {
		NodeContent node = NodeContentFinder.findOne(NodeContentFinder.id().eq(result.getNodeId()));

		if (node != null) {
			result.setName(HtmlStripper.stripHtmlTags(node.getName()));
			result.setNote(node.getNote() != null ? HtmlStripper.stripHtmlTags(node.getNote()) : null);
			result.setFullPath(this.pathBuilder.buildFullPath(result.getNodeId()));
			result.setTextContent(this.pathBuilder.buildTextContent(result.getNodeId()));
		}
	}
}
