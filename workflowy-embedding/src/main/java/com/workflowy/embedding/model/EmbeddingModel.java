package com.workflowy.embedding.model;

import java.util.Optional;

public enum EmbeddingModel
{
    MINILM(
            "minilm",
            "sentence-transformers/all-MiniLM-L6-v2",
            384,
            0.7,
            ModelType.LOCAL,
            Optional.empty(),
            Optional.empty()),

    MPNET(
            "mpnet",
            "sentence-transformers/all-mpnet-base-v2",
            768,
            0.8,
            ModelType.LOCAL,
            Optional.empty(),
            Optional.empty()),

    BGE(
            "bge",
            "BAAI/bge-large-en-v1.5",
            1024,
            0.5,
            ModelType.LOCAL,
            Optional.of("Represent this sentence for searching relevant passages: "),
            Optional.of("")),

    OPENAI_SMALL(
            "openai-small",
            "text-embedding-3-small",
            1536,
            0.5,
            ModelType.OPENAI,
            Optional.empty(),
            Optional.empty()),

    OPENAI_LARGE(
            "openai-large",
            "text-embedding-3-large",
            3072,
            0.5,
            ModelType.OPENAI,
            Optional.empty(),
            Optional.empty());

    private final String key;
    private final String modelName;
    private final int dimensions;
    private final double defaultThreshold;
    private final ModelType modelType;
    private final Optional<String> queryPrefix;
    private final Optional<String> passagePrefix;

    EmbeddingModel(
            String key,
            String modelName,
            int dimensions,
            double defaultThreshold,
            ModelType modelType,
            Optional<String> queryPrefix,
            Optional<String> passagePrefix)
    {
        this.key = key;
        this.modelName = modelName;
        this.dimensions = dimensions;
        this.defaultThreshold = defaultThreshold;
        this.modelType = modelType;
        this.queryPrefix = queryPrefix;
        this.passagePrefix = passagePrefix;
    }

    public String getKey()
    {
        return this.key;
    }

    public String getModelName()
    {
        return this.modelName;
    }

    public int getDimensions()
    {
        return this.dimensions;
    }

    public double getDefaultThreshold()
    {
        return this.defaultThreshold;
    }

    public ModelType getModelType()
    {
        return this.modelType;
    }

    public Optional<String> getQueryPrefix()
    {
        return this.queryPrefix;
    }

    public Optional<String> getPassagePrefix()
    {
        return this.passagePrefix;
    }

    public boolean isLocal()
    {
        return this.modelType == ModelType.LOCAL;
    }

    public boolean isOpenAI()
    {
        return this.modelType == ModelType.OPENAI;
    }

    public static EmbeddingModel fromKey(String key)
    {
        for (EmbeddingModel model : values())
        {
            if (model.key.equals(key))
            {
                return model;
            }
        }
        throw new IllegalArgumentException("Unknown embedding model: " + key);
    }

    public enum ModelType
    {
        LOCAL,
        OPENAI
    }
}
