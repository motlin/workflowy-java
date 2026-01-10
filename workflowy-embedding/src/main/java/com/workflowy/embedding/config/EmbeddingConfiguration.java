package com.workflowy.embedding.config;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EmbeddingConfiguration
{
    @Nullable
    private String openaiApiKey;

    @NotNull
    private String defaultModel = "minilm";

    @NotNull
    private String databasePath = "./embeddings.sqlite";

    @NotNull
    private String modelCachePath = "./models";

    @JsonProperty("openaiApiKey")
    @Nullable
    public String getOpenaiApiKey()
    {
        return this.openaiApiKey;
    }

    @JsonProperty("openaiApiKey")
    public void setOpenaiApiKey(@Nullable String openaiApiKey)
    {
        this.openaiApiKey = openaiApiKey;
    }

    @JsonProperty("defaultModel")
    @NotNull
    public String getDefaultModel()
    {
        return this.defaultModel;
    }

    @JsonProperty("defaultModel")
    public void setDefaultModel(@NotNull String defaultModel)
    {
        this.defaultModel = defaultModel;
    }

    @JsonProperty("databasePath")
    @NotNull
    public String getDatabasePath()
    {
        return this.databasePath;
    }

    @JsonProperty("databasePath")
    public void setDatabasePath(@NotNull String databasePath)
    {
        this.databasePath = databasePath;
    }

    @JsonProperty("modelCachePath")
    @NotNull
    public String getModelCachePath()
    {
        return this.modelCachePath;
    }

    @JsonProperty("modelCachePath")
    public void setModelCachePath(@NotNull String modelCachePath)
    {
        this.modelCachePath = modelCachePath;
    }
}
