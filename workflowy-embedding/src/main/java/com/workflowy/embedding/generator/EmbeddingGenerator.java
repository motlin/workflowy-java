package com.workflowy.embedding.generator;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.workflowy.NodeContent;
import com.workflowy.NodeContentFinder;
import com.workflowy.NodeContentList;
import com.workflowy.embedding.engine.EmbeddingEngine;
import com.workflowy.embedding.model.EmbeddingModel;
import com.workflowy.embedding.model.NodeEmbedding;
import com.workflowy.embedding.repository.EmbeddingRepository;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddingGenerator {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingGenerator.class);

	private static final Instant FAR_FUTURE = Instant.parse("9999-12-31T23:59:59Z");

	private final EmbeddingEngine engine;
	private final EmbeddingRepository repository;
	private final PathBuilder pathBuilder;
	private final int batchSize;
	private final boolean force;

	public EmbeddingGenerator(EmbeddingEngine engine, EmbeddingRepository repository, int batchSize, boolean force) {
		this.engine = engine;
		this.repository = repository;
		this.pathBuilder = new PathBuilder();
		this.batchSize = batchSize;
		this.force = force;
	}

	public GenerationResult generate(Consumer<ProgressUpdate> progressCallback) {
		EmbeddingModel model = this.engine.getModel();

		Set<String> existingNodeIds;
		try {
			existingNodeIds = this.force ? Set.of() : this.repository.getExistingNodeIds(model);
		} catch (SQLException e) {
			throw new RuntimeException("Failed to get existing node IDs", e);
		}

		NodeContentList allNodes = NodeContentFinder.findMany(NodeContentFinder.all());
		int totalNodes = allNodes.size();
		int skippedCount = 0;
		int processedCount = 0;
		int errorCount = 0;

		MutableList<NodeContent> batch = Lists.mutable.withInitialCapacity(this.batchSize);

		for (int i = 0; i < allNodes.size(); i++) {
			NodeContent node = allNodes.get(i);

			if (!this.force && existingNodeIds.contains(node.getId())) {
				skippedCount++;
				continue;
			}

			batch.add(node);

			if (batch.size() >= this.batchSize || i == allNodes.size() - 1) {
				try {
					this.processBatch(batch, model);
					processedCount += batch.size();

					if (progressCallback != null) {
						progressCallback.accept(
							new ProgressUpdate(
								processedCount + skippedCount,
								totalNodes,
								processedCount,
								skippedCount,
								errorCount
							)
						);
					}
				} catch (Exception e) {
					LOGGER.error("Error processing batch", e);
					errorCount += batch.size();
				}

				batch.clear();
			}
		}

		return new GenerationResult(totalNodes, processedCount, skippedCount, errorCount);
	}

	private void processBatch(List<NodeContent> nodes, EmbeddingModel model) throws SQLException {
		List<String> texts = nodes
			.stream()
			.map((node) -> this.pathBuilder.buildEmbeddingText(node.getId()))
			.toList();

		List<float[]> embeddings = this.engine.generateEmbeddings(texts, false);

		MutableList<NodeEmbedding> nodeEmbeddings = Lists.mutable.empty();
		for (int i = 0; i < nodes.size(); i++) {
			NodeContent node = nodes.get(i);
			float[] embedding = embeddings.get(i);

			NodeEmbedding nodeEmbedding = new NodeEmbedding(
				node.getId(),
				model.getKey(),
				embedding,
				Instant.now(),
				FAR_FUTURE
			);
			nodeEmbeddings.add(nodeEmbedding);
		}

		this.repository.saveBatch(nodeEmbeddings);
	}

	public record ProgressUpdate(int current, int total, int processed, int skipped, int errors) {
		public int percentage() {
			return this.total > 0 ? (this.current * 100) / this.total : 0;
		}
	}

	public record GenerationResult(int totalNodes, int processedCount, int skippedCount, int errorCount) {}
}
