package com.workflowy.embedding.engine;

import java.util.List;

import com.workflowy.embedding.model.EmbeddingModel;

public interface EmbeddingEngine extends AutoCloseable
{
    float[] generateEmbedding(String text, boolean isQuery);

    List<float[]> generateEmbeddings(List<String> texts, boolean isQuery);

    EmbeddingModel getModel();

    @Override
    void close();
}
