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

@Tag("integration")
class OnnxEmbeddingEngineTest
{
    private static OnnxEmbeddingEngine engine;
    private static Path modelCachePath;

    @BeforeAll
    static void setUpClass() throws IOException
    {
        modelCachePath = Files.createTempDirectory("onnx-test-cache");
        engine = new OnnxEmbeddingEngine(EmbeddingModel.MINILM, modelCachePath.toString());
    }

    @AfterAll
    static void tearDownClass()
    {
        if (engine != null)
        {
            engine.close();
        }
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
        float[] embedding = engine.generateEmbedding("Hello world", false);

        assertNotNull(embedding);
        assertEquals(EmbeddingModel.MINILM.getDimensions(), embedding.length);
    }

    @Test
    void generateEmbedding_withLongText_returnsCorrectDimensions()
    {
        String longText = "This is a very long text that contains many words. ".repeat(50);

        float[] embedding = engine.generateEmbedding(longText, false);

        assertNotNull(embedding);
        assertEquals(EmbeddingModel.MINILM.getDimensions(), embedding.length);
    }

    @Test
    void generateEmbedding_outputIsNormalized()
    {
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
        float[] embedding1 = engine.generateEmbedding("The cat sat on the mat", false);
        float[] embedding2 = engine.generateEmbedding("A cat is sitting on a mat", false);
        float[] embedding3 = engine.generateEmbedding("Quantum physics and thermodynamics", false);

        double similarityCats = cosineSimilarity(embedding1, embedding2);
        double similarityDifferent = cosineSimilarity(embedding1, embedding3);

        assertTrue(
                similarityCats > similarityDifferent,
                String.format(
                        "Similar sentences (%.3f) should have higher similarity than unrelated ones (%.3f)",
                        similarityCats,
                        similarityDifferent));
        assertTrue(
                similarityCats > 0.7,
                String.format("Very similar sentences should have similarity > 0.7, got %.3f", similarityCats));
        assertTrue(
                similarityDifferent < similarityCats,
                String.format("Unrelated sentences (%.3f) should have lower similarity than related ones (%.3f)",
                        similarityDifferent,
                        similarityCats));
    }

    @Test
    void generateEmbeddings_withMultipleTexts_returnsAll()
    {
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
        String text = "Deterministic embedding test";

        float[] embedding1 = engine.generateEmbedding(text, false);
        float[] embedding2 = engine.generateEmbedding(text, false);

        assertArrayEquals(embedding1, embedding2, 0.0001f);
    }

    @Test
    void generateEmbedding_queryVsPassage_producesSlightlyDifferentResults()
    {
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
