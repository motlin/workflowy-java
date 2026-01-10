package com.workflowy.embedding.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

import com.workflowy.embedding.model.EmbeddingModel;
import com.workflowy.embedding.model.NodeEmbedding;
import com.workflowy.embedding.repository.SqliteVecConnection;

public final class EmbeddingTestHelper
{
    private static final Random RANDOM = new Random(42);

    private EmbeddingTestHelper()
    {
    }

    public static Path createTempDatabasePath() throws IOException
    {
        Path tempFile = Files.createTempFile("test-embeddings-", ".db");
        Files.delete(tempFile);
        return tempFile;
    }

    public static SqliteVecConnection createTempDatabase() throws IOException, SQLException
    {
        Path dbPath = createTempDatabasePath();
        return new SqliteVecConnection(dbPath.toString());
    }

    public static float[] createRandomEmbedding(int dimensions)
    {
        float[] embedding = new float[dimensions];
        for (int i = 0; i < dimensions; i++)
        {
            embedding[i] = RANDOM.nextFloat() * 2 - 1;
        }
        return normalize(embedding);
    }

    public static float[] createSimilarEmbedding(float[] original, float variance)
    {
        float[] similar = new float[original.length];
        for (int i = 0; i < original.length; i++)
        {
            similar[i] = original[i] + (RANDOM.nextFloat() * 2 - 1) * variance;
        }
        return normalize(similar);
    }

    public static float[] normalize(float[] vector)
    {
        float magnitude = 0;
        for (float v : vector)
        {
            magnitude += v * v;
        }
        magnitude = (float) Math.sqrt(magnitude);

        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++)
        {
            normalized[i] = vector[i] / magnitude;
        }
        return normalized;
    }

    public static NodeEmbedding createNodeEmbedding(String nodeId, EmbeddingModel model)
    {
        return new NodeEmbedding(
                nodeId,
                model.getKey(),
                createRandomEmbedding(model.getDimensions()),
                Instant.now(),
                Instant.parse("9999-12-31T23:59:59Z"));
    }

    public static NodeEmbedding createNodeEmbedding(String nodeId, EmbeddingModel model, float[] embedding)
    {
        return new NodeEmbedding(
                nodeId,
                model.getKey(),
                embedding,
                Instant.now(),
                Instant.parse("9999-12-31T23:59:59Z"));
    }

    public static String randomNodeId()
    {
        return UUID.randomUUID().toString();
    }
}
