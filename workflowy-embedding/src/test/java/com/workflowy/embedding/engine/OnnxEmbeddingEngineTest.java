package com.workflowy.embedding.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.workflowy.embedding.model.EmbeddingModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
class OnnxEmbeddingEngineTest
{
    private static OnnxEmbeddingEngine engine;
    private static Path modelCachePath;
    private static boolean modelAvailable;

    @BeforeAll
    static void setUpClass() throws IOException
    {
        modelCachePath = Files.createTempDirectory("onnx-test-cache");

        try
        {
            engine = new OnnxEmbeddingEngine(EmbeddingModel.MINILM, modelCachePath.toString());
            modelAvailable = true;
        }
        catch (Exception e)
        {
            System.err.println("ONNX model not available: " + e.getMessage());
            System.err.println("Skipping OnnxEmbeddingEngineTest integration tests.");
            System.err.println("To enable these tests, ensure the DJL HuggingFace ONNX model zoo is available.");
            modelAvailable = false;
        }
    }

    @AfterAll
    static void tearDownClass()
    {
        if (engine != null)
        {
            engine.close();
        }
    }

    private void assumeModelAvailable()
    {
        assumeTrue(modelAvailable, "ONNX model not available - skipping test");
    }

    @Test
    void constructor_withOpenAIModel_throwsException()
    {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OnnxEmbeddingEngine(EmbeddingModel.OPENAI_SMALL, modelCachePath.toString()));
    }

    @Test
    void generateEmbedding_withShortText_returnsCorrectDimensions()
    {
        this.assumeModelAvailable();

        float[] embedding = engine.generateEmbedding("Hello world", false);

        assertNotNull(embedding);
        assertEquals(EmbeddingModel.MINILM.getDimensions(), embedding.length);
    }

    @Test
    void generateEmbedding_withLongText_returnsCorrectDimensions()
    {
        this.assumeModelAvailable();

        String longText = "This is a very long text that contains many words. ".repeat(50);

        float[] embedding = engine.generateEmbedding(longText, false);

        assertNotNull(embedding);
        assertEquals(EmbeddingModel.MINILM.getDimensions(), embedding.length);
    }

    @Test
    void generateEmbedding_outputIsNormalized()
    {
        this.assumeModelAvailable();

        float[] embedding = engine.generateEmbedding("Test sentence for normalization", false);

        double norm = 0.0;
        for (float value : embedding)
        {
            norm += value * value;
        }
        norm = Math.sqrt(norm);

        assertEquals(1.0, norm, 0.01);
    }

    @Test
    void generateEmbedding_similarTextProducesSimilarEmbeddings()
    {
        this.assumeModelAvailable();

        float[] embedding1 = engine.generateEmbedding("The cat sat on the mat", false);
        float[] embedding2 = engine.generateEmbedding("A cat is sitting on a mat", false);
        float[] embedding3 = engine.generateEmbedding("Quantum physics and thermodynamics", false);

        double similarityCats = cosineSimilarity(embedding1, embedding2);
        double similarityDifferent = cosineSimilarity(embedding1, embedding3);

        assertTrue(
                similarityCats > similarityDifferent,
                "Similar sentences should have higher similarity than unrelated ones");
        assertTrue(similarityCats > 0.8, "Very similar sentences should have similarity > 0.8");
        assertTrue(similarityDifferent < 0.5, "Unrelated sentences should have similarity < 0.5");
    }

    @Test
    void generateEmbeddings_withMultipleTexts_returnsAll()
    {
        this.assumeModelAvailable();

        List<String> texts = List.of("First sentence", "Second sentence", "Third sentence");

        List<float[]> embeddings = engine.generateEmbeddings(texts, false);

        assertEquals(3, embeddings.size());
        for (float[] embedding : embeddings)
        {
            assertEquals(EmbeddingModel.MINILM.getDimensions(), embedding.length);
        }
    }

    @Test
    void generateEmbedding_sameTextProducesSameEmbedding()
    {
        this.assumeModelAvailable();

        String text = "Deterministic embedding test";

        float[] embedding1 = engine.generateEmbedding(text, false);
        float[] embedding2 = engine.generateEmbedding(text, false);

        assertArrayEquals(embedding1, embedding2, 0.0001f);
    }

    @Test
    void generateEmbedding_queryVsPassage_producesSlightlyDifferentResults()
    {
        this.assumeModelAvailable();

        String text = "Search query about machine learning";

        float[] queryEmbedding = engine.generateEmbedding(text, true);
        float[] passageEmbedding = engine.generateEmbedding(text, false);

        assertArrayEquals(
                queryEmbedding,
                passageEmbedding,
                0.0001f,
                "MINILM has no query/passage prefix, so embeddings should be identical");
    }

    @Test
    void getModel_returnsConfiguredModel()
    {
        this.assumeModelAvailable();

        assertEquals(EmbeddingModel.MINILM, engine.getModel());
    }

    private static double cosineSimilarity(float[] a, float[] b)
    {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++)
        {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
