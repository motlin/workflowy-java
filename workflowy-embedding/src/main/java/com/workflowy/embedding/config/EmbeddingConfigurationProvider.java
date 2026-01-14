package com.workflowy.embedding.config;

import javax.annotation.Nonnull;

public interface EmbeddingConfigurationProvider {
	@Nonnull
	EmbeddingConfiguration getEmbeddingConfiguration();
}
