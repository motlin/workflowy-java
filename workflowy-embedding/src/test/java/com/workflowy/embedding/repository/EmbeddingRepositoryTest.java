package com.workflowy.embedding.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.workflowy.embedding.model.EmbeddingModel;
import com.workflowy.embedding.model.NodeEmbedding;
import com.workflowy.embedding.search.SearchResult;
import com.workflowy.embedding.test.EmbeddingTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddingRepositoryTest
{
    private SqliteVecConnection connection;
    private EmbeddingRepository repository;

    @BeforeEach
    void setUp() throws Exception
    {
        this.connection = EmbeddingTestHelper.createTempDatabase();
        this.repository = new EmbeddingRepository(this.connection);
    }

    @AfterEach
    void tearDown() throws Exception
    {
        if (this.connection != null)
        {
            this.connection.close();
        }
    }

    @Test
    void save_withNewEmbedding_insertsRecord() throws SQLException
    {
        String nodeId = EmbeddingTestHelper.randomNodeId();
        NodeEmbedding embedding = EmbeddingTestHelper.createNodeEmbedding(nodeId, EmbeddingModel.MINILM);

        this.repository.save(embedding);

        Set<String> existingIds = this.repository.getExistingNodeIds(EmbeddingModel.MINILM);
        assertTrue(existingIds.contains(nodeId));
    }

    @Test
    void saveBatch_withMultipleEmbeddings_insertsAll() throws SQLException
    {
        String nodeId1 = EmbeddingTestHelper.randomNodeId();
        String nodeId2 = EmbeddingTestHelper.randomNodeId();
        String nodeId3 = EmbeddingTestHelper.randomNodeId();

        List<NodeEmbedding> embeddings = List.of(
                EmbeddingTestHelper.createNodeEmbedding(nodeId1, EmbeddingModel.MINILM),
                EmbeddingTestHelper.createNodeEmbedding(nodeId2, EmbeddingModel.MINILM),
                EmbeddingTestHelper.createNodeEmbedding(nodeId3, EmbeddingModel.MINILM));

        this.repository.saveBatch(embeddings);

        Set<String> existingIds = this.repository.getExistingNodeIds(EmbeddingModel.MINILM);
        assertEquals(3, existingIds.size());
        assertTrue(existingIds.contains(nodeId1));
        assertTrue(existingIds.contains(nodeId2));
        assertTrue(existingIds.contains(nodeId3));
    }

    @Test
    void saveBatch_withEmptyList_succeeds() throws SQLException
    {
        this.repository.saveBatch(List.of());

        Set<String> existingIds = this.repository.getExistingNodeIds(EmbeddingModel.MINILM);
        assertTrue(existingIds.isEmpty());
    }

    @Test
    void getExistingNodeIds_withNoEmbeddings_returnsEmptySet() throws SQLException
    {
        Set<String> existingIds = this.repository.getExistingNodeIds(EmbeddingModel.MINILM);
        assertTrue(existingIds.isEmpty());
    }

    @Test
    void getExistingNodeIds_withEmbeddings_returnsNodeIds() throws SQLException
    {
        String nodeId1 = EmbeddingTestHelper.randomNodeId();
        String nodeId2 = EmbeddingTestHelper.randomNodeId();

        this.repository.save(EmbeddingTestHelper.createNodeEmbedding(nodeId1, EmbeddingModel.MINILM));
        this.repository.save(EmbeddingTestHelper.createNodeEmbedding(nodeId2, EmbeddingModel.MINILM));

        Set<String> existingIds = this.repository.getExistingNodeIds(EmbeddingModel.MINILM);
        assertEquals(2, existingIds.size());
        assertTrue(existingIds.contains(nodeId1));
        assertTrue(existingIds.contains(nodeId2));
    }

    @Test
    void getExistingNodeIds_filtersByModel() throws SQLException
    {
        String nodeId1 = EmbeddingTestHelper.randomNodeId();
        String nodeId2 = EmbeddingTestHelper.randomNodeId();

        this.repository.save(EmbeddingTestHelper.createNodeEmbedding(nodeId1, EmbeddingModel.MINILM));
        this.repository.save(EmbeddingTestHelper.createNodeEmbedding(nodeId2, EmbeddingModel.MPNET));

        Set<String> minilmIds = this.repository.getExistingNodeIds(EmbeddingModel.MINILM);
        Set<String> mpnetIds = this.repository.getExistingNodeIds(EmbeddingModel.MPNET);

        assertEquals(1, minilmIds.size());
        assertTrue(minilmIds.contains(nodeId1));

        assertEquals(1, mpnetIds.size());
        assertTrue(mpnetIds.contains(nodeId2));
    }

    @Test
    void search_withoutVecExtension_throwsSqlException()
    {
        float[] queryEmbedding = EmbeddingTestHelper.createRandomEmbedding(EmbeddingModel.MINILM.getDimensions());

        if (!this.connection.isSqliteVecLoaded())
        {
            assertThrows(SQLException.class, () ->
                    this.repository.search(queryEmbedding, EmbeddingModel.MINILM, 5, null));
        }
    }

    @Test
    void search_withVecExtension_returnsResults() throws SQLException
    {
        if (!this.connection.isSqliteVecLoaded())
        {
            return;
        }

        float[] baseEmbedding = EmbeddingTestHelper.createRandomEmbedding(EmbeddingModel.MINILM.getDimensions());
        String nodeId1 = EmbeddingTestHelper.randomNodeId();
        String nodeId2 = EmbeddingTestHelper.randomNodeId();

        float[] similar1 = EmbeddingTestHelper.createSimilarEmbedding(baseEmbedding, 0.1f);
        float[] similar2 = EmbeddingTestHelper.createSimilarEmbedding(baseEmbedding, 0.2f);

        this.repository.save(EmbeddingTestHelper.createNodeEmbedding(nodeId1, EmbeddingModel.MINILM, similar1));
        this.repository.save(EmbeddingTestHelper.createNodeEmbedding(nodeId2, EmbeddingModel.MINILM, similar2));

        List<SearchResult> results = this.repository.search(baseEmbedding, EmbeddingModel.MINILM, 10, null);

        assertEquals(2, results.size());
        assertTrue(results.get(0).getDistance() <= results.get(1).getDistance());
    }

    @Test
    void search_withLimit_respectsLimit() throws SQLException
    {
        if (!this.connection.isSqliteVecLoaded())
        {
            return;
        }

        float[] baseEmbedding = EmbeddingTestHelper.createRandomEmbedding(EmbeddingModel.MINILM.getDimensions());

        for (int i = 0; i < 10; i++)
        {
            String nodeId = EmbeddingTestHelper.randomNodeId();
            float[] similar = EmbeddingTestHelper.createSimilarEmbedding(baseEmbedding, 0.1f * (i + 1));
            this.repository.save(EmbeddingTestHelper.createNodeEmbedding(nodeId, EmbeddingModel.MINILM, similar));
        }

        List<SearchResult> results = this.repository.search(baseEmbedding, EmbeddingModel.MINILM, 3, null);

        assertEquals(3, results.size());
    }

    @Test
    void search_withThreshold_filtersResults() throws SQLException
    {
        if (!this.connection.isSqliteVecLoaded())
        {
            return;
        }

        float[] baseEmbedding = EmbeddingTestHelper.createRandomEmbedding(EmbeddingModel.MINILM.getDimensions());

        String closeNodeId = EmbeddingTestHelper.randomNodeId();
        float[] closeEmbedding = EmbeddingTestHelper.createSimilarEmbedding(baseEmbedding, 0.05f);
        this.repository.save(EmbeddingTestHelper.createNodeEmbedding(closeNodeId, EmbeddingModel.MINILM, closeEmbedding));

        String farNodeId = EmbeddingTestHelper.randomNodeId();
        float[] farEmbedding = EmbeddingTestHelper.createRandomEmbedding(EmbeddingModel.MINILM.getDimensions());
        this.repository.save(EmbeddingTestHelper.createNodeEmbedding(farNodeId, EmbeddingModel.MINILM, farEmbedding));

        List<SearchResult> results = this.repository.search(baseEmbedding, EmbeddingModel.MINILM, 10, 0.2);

        assertFalse(results.isEmpty());
        for (SearchResult result : results)
        {
            assertTrue(result.getDistance() < 0.2);
        }
    }
}
