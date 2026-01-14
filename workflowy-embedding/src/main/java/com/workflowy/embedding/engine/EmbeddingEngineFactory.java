package com.workflowy.embedding.engine;

import javax.annotation.Nullable;

import com.workflowy.embedding.config.EmbeddingConfiguration;
import com.workflowy.embedding.model.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EmbeddingEngineFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingEngineFactory.class);

	private EmbeddingEngineFactory() {}

	public static EmbeddingEngine create(EmbeddingModel model, EmbeddingConfiguration configuration) {
		return create(model, configuration.getOpenaiApiKey(), configuration.getModelCachePath());
	}

	public static EmbeddingEngine create(EmbeddingModel model, @Nullable String openaiApiKey, String modelCachePath) {
		if (model.isOpenAI()) {
			if (openaiApiKey == null || openaiApiKey.isBlank()) {
				throw new IllegalArgumentException(
					"OpenAI API key is required for OpenAI models. "
					+ "Set the OPENAI_API_KEY environment variable or configure it in config.json5"
				);
			}
			LOGGER.info("Creating OpenAI embedding engine for model: {}", model.getKey());
			return new OpenAIEmbeddingEngine(model, openaiApiKey);
		} else {
			LOGGER.info("Creating ONNX embedding engine for model: {}", model.getKey());
			return new OnnxEmbeddingEngine(model, modelCachePath);
		}
	}
}
