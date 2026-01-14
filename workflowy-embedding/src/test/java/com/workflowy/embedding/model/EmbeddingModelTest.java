package com.workflowy.embedding.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddingModelTest
{
    @Test
    void minilm_hasCorrectProperties()
    {
        EmbeddingModel model = EmbeddingModel.MINILM;
        assertEquals("minilm", model.getKey());
        assertEquals("sentence-transformers/all-MiniLM-L6-v2", model.getModelName());
        assertEquals(384, model.getDimensions());
        assertEquals(0.7, model.getDefaultThreshold());
        assertTrue(model.isLocal());
        assertFalse(model.isOpenAI());
    }

    @Test
    void mpnet_hasCorrectProperties()
    {
        EmbeddingModel model = EmbeddingModel.MPNET;
        assertEquals("mpnet", model.getKey());
        assertEquals("sentence-transformers/all-mpnet-base-v2", model.getModelName());
        assertEquals(768, model.getDimensions());
        assertEquals(0.8, model.getDefaultThreshold());
        assertTrue(model.isLocal());
        assertFalse(model.isOpenAI());
    }

    @Test
    void bge_hasCorrectProperties()
    {
        EmbeddingModel model = EmbeddingModel.BGE;
        assertEquals("bge", model.getKey());
        assertEquals("BAAI/bge-large-en-v1.5", model.getModelName());
        assertEquals(1024, model.getDimensions());
        assertEquals(0.5, model.getDefaultThreshold());
        assertTrue(model.isLocal());
        assertFalse(model.isOpenAI());
        assertTrue(model.getQueryPrefix().isPresent());
        assertTrue(model.getPassagePrefix().isPresent());
    }

    @Test
    void openaiSmall_hasCorrectProperties()
    {
        EmbeddingModel model = EmbeddingModel.OPENAI_SMALL;
        assertEquals("openai-small", model.getKey());
        assertEquals("text-embedding-3-small", model.getModelName());
        assertEquals(1536, model.getDimensions());
        assertEquals(0.5, model.getDefaultThreshold());
        assertFalse(model.isLocal());
        assertTrue(model.isOpenAI());
    }

    @Test
    void openaiLarge_hasCorrectProperties()
    {
        EmbeddingModel model = EmbeddingModel.OPENAI_LARGE;
        assertEquals("openai-large", model.getKey());
        assertEquals("text-embedding-3-large", model.getModelName());
        assertEquals(3072, model.getDimensions());
        assertEquals(0.5, model.getDefaultThreshold());
        assertFalse(model.isLocal());
        assertTrue(model.isOpenAI());
    }

    @Test
    void fromKey_withValidKey_returnsModel()
    {
        assertEquals(EmbeddingModel.MINILM, EmbeddingModel.fromKey("minilm"));
        assertEquals(EmbeddingModel.MPNET, EmbeddingModel.fromKey("mpnet"));
        assertEquals(EmbeddingModel.BGE, EmbeddingModel.fromKey("bge"));
        assertEquals(EmbeddingModel.OPENAI_SMALL, EmbeddingModel.fromKey("openai-small"));
        assertEquals(EmbeddingModel.OPENAI_LARGE, EmbeddingModel.fromKey("openai-large"));
    }

    @Test
    void fromKey_withInvalidKey_throwsException()
    {
        assertThrows(IllegalArgumentException.class, () -> EmbeddingModel.fromKey("invalid"));
    }

    @Test
    void localModels_haveNoQueryPrefixByDefault()
    {
        assertFalse(EmbeddingModel.MINILM.getQueryPrefix().isPresent());
        assertFalse(EmbeddingModel.MPNET.getQueryPrefix().isPresent());
    }

    @Test
    void bge_hasQueryPrefixForSearchOptimization()
    {
        assertTrue(EmbeddingModel.BGE.getQueryPrefix().isPresent());
        assertEquals("Represent this sentence for searching relevant passages: ",
                EmbeddingModel.BGE.getQueryPrefix().get());
    }

    @Test
    void allModels_haveDimensionsGreaterThanZero()
    {
        for (EmbeddingModel model : EmbeddingModel.values())
        {
            assertTrue(model.getDimensions() > 0,
                    "Model " + model.getKey() + " should have positive dimensions");
        }
    }

    @Test
    void allModels_haveDefaultThresholdBetweenZeroAndOne()
    {
        for (EmbeddingModel model : EmbeddingModel.values())
        {
            assertTrue(model.getDefaultThreshold() > 0 && model.getDefaultThreshold() <= 1,
                    "Model " + model.getKey() + " should have threshold between 0 and 1");
        }
    }
}
