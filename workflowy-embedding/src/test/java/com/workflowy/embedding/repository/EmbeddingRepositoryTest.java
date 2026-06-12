package com.workflowy.embedding.repository;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddingRepositoryTest {

	private SqliteVecConnection connection;
	private EmbeddingRepository repository;

	@BeforeEach
	void setUp() throws Exception {
		this.connection = EmbeddingTestHelper.createTempDatabase();
		this.repository = new EmbeddingRepository(this.connection);
	}

	@AfterEach
	void tearDown() throws Exception {
		if (this.connection != null) {
			this.connection.close();
		}
	}

	@Test
	void save_withNewEmbedding_insertsRecord() throws SQLException {
		String nodeId = EmbeddingTestHelper.randomNodeId();
		NodeEmbedding embedding = EmbeddingTestHelper.createNodeEmbedding(nodeId, EmbeddingModel.MINILM);

		this.repository.save(embedding);

		Set<String> existingIds = this.repository.getExistingNodeIds(EmbeddingModel.MINILM);
		assertTrue(existingIds.contains(nodeId));
	}

	@Test
	void saveBatch_withMultipleEmbeddings_insertsAll() throws SQLException {
		String nodeId1 = EmbeddingTestHelper.randomNodeId();
		String nodeId2 = EmbeddingTestHelper.randomNodeId();
		String nodeId3 = EmbeddingTestHelper.randomNodeId();

		List<NodeEmbedding> embeddings = List.of(
			EmbeddingTestHelper.createNodeEmbedding(nodeId1, EmbeddingModel.MINILM),
			EmbeddingTestHelper.createNodeEmbedding(nodeId2, EmbeddingModel.MINILM),
			EmbeddingTestHelper.createNodeEmbedding(nodeId3, EmbeddingModel.MINILM)
		);

		this.repository.saveBatch(embeddings);

		Set<String> existingIds = this.repository.getExistingNodeIds(EmbeddingModel.MINILM);
		assertEquals(3, existingIds.size());
		assertTrue(existingIds.contains(nodeId1));
		assertTrue(existingIds.contains(nodeId2));
		assertTrue(existingIds.contains(nodeId3));
	}

	@Test
	void saveBatch_withEmptyList_succeeds() throws SQLException {
		this.repository.saveBatch(List.of());

		Set<String> existingIds = this.repository.getExistingNodeIds(EmbeddingModel.MINILM);
		assertTrue(existingIds.isEmpty());
	}

	@Test
	void getExistingNodeIds_withNoEmbeddings_returnsEmptySet() throws SQLException {
		Set<String> existingIds = this.repository.getExistingNodeIds(EmbeddingModel.MINILM);
		assertTrue(existingIds.isEmpty());
	}

	@Test
	void getExistingNodeIds_withEmbeddings_returnsNodeIds() throws SQLException {
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
	void getExistingNodeIds_filtersByModel() throws SQLException {
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
	void search_withoutVecExtension_throwsSqlException() {
		float[] queryEmbedding = EmbeddingTestHelper.createRandomEmbedding(EmbeddingModel.MINILM.getDimensions());

		if (!this.connection.isSqliteVecLoaded()) {
			assertThrows(SQLException.class, () ->
				this.repository.search(queryEmbedding, EmbeddingModel.MINILM, 5, null)
			);
		}
	}

	@Test
	void search_withVecExtension_returnsResults() throws SQLException {
		if (!this.connection.isSqliteVecLoaded()) {
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
		assertTrue(results.getFirst().getDistance() <= results.get(1).getDistance());
	}

	@Test
	void search_withLimit_respectsLimit() throws SQLException {
		if (!this.connection.isSqliteVecLoaded()) {
			return;
		}

		float[] baseEmbedding = EmbeddingTestHelper.createRandomEmbedding(EmbeddingModel.MINILM.getDimensions());

		for (var i = 0; i < 10; i++) {
			String nodeId = EmbeddingTestHelper.randomNodeId();
			float[] similar = EmbeddingTestHelper.createSimilarEmbedding(baseEmbedding, 0.1f * (i + 1));
			this.repository.save(EmbeddingTestHelper.createNodeEmbedding(nodeId, EmbeddingModel.MINILM, similar));
		}

		List<SearchResult> results = this.repository.search(baseEmbedding, EmbeddingModel.MINILM, 3, null);

		assertEquals(3, results.size());
	}

	@Test
	void populateFts_insertsContent() throws SQLException {
		String nodeId1 = EmbeddingTestHelper.randomNodeId();
		String nodeId2 = EmbeddingTestHelper.randomNodeId();

		Map<String, String> contents = new LinkedHashMap<>();
		contents.put(nodeId1, "PATH: Root > Projects\nCONTENT: My Project\n\nProject notes");
		contents.put(nodeId2, "PATH: Root > Tasks\nCONTENT: Buy groceries\n\nMilk and eggs");

		this.repository.populateFts(contents);

		List<SearchResult> results = this.repository.searchKeyword("groceries", 10);
		assertEquals(1, results.size());
		assertEquals(nodeId2, results.getFirst().getNodeId());
	}

	@Test
	void populateFts_clearsExistingContent() throws SQLException {
		String nodeId1 = EmbeddingTestHelper.randomNodeId();
		String nodeId2 = EmbeddingTestHelper.randomNodeId();

		Map<String, String> firstBatch = new LinkedHashMap<>();
		firstBatch.put(nodeId1, "First batch content about apples");
		this.repository.populateFts(firstBatch);

		Map<String, String> secondBatch = new LinkedHashMap<>();
		secondBatch.put(nodeId2, "Second batch content about oranges");
		this.repository.populateFts(secondBatch);

		List<SearchResult> appleResults = this.repository.searchKeyword("apples", 10);
		assertTrue(appleResults.isEmpty());

		List<SearchResult> orangeResults = this.repository.searchKeyword("oranges", 10);
		assertEquals(1, orangeResults.size());
		assertEquals(nodeId2, orangeResults.getFirst().getNodeId());
	}

	@Test
	void searchKeyword_withNoResults_returnsEmptyList() throws SQLException {
		Map<String, String> contents = new LinkedHashMap<>();
		contents.put(EmbeddingTestHelper.randomNodeId(), "Some content about programming");
		this.repository.populateFts(contents);

		List<SearchResult> results = this.repository.searchKeyword("zyxwvutsrqp", 10);
		assertTrue(results.isEmpty());
	}

	@Test
	void searchKeyword_respectsLimit() throws SQLException {
		Map<String, String> contents = new LinkedHashMap<>();
		for (var i = 0; i < 10; i++) {
			contents.put(EmbeddingTestHelper.randomNodeId(), "Document about testing software quality " + i);
		}
		this.repository.populateFts(contents);

		List<SearchResult> results = this.repository.searchKeyword("testing", 3);
		assertEquals(3, results.size());
	}

	@Test
	void searchKeyword_ranksResults() throws SQLException {
		String exactId = EmbeddingTestHelper.randomNodeId();
		String partialId = EmbeddingTestHelper.randomNodeId();

		Map<String, String> contents = new LinkedHashMap<>();
		contents.put(exactId, "Java programming Java development Java testing");
		contents.put(partialId, "Python development with some Java");
		this.repository.populateFts(contents);

		List<SearchResult> results = this.repository.searchKeyword("java", 10);
		assertEquals(2, results.size());
		assertEquals(exactId, results.getFirst().getNodeId());
	}

	@Test
	void searchKeyword_distanceIsBetweenZeroAndOne() throws SQLException {
		Map<String, String> contents = new LinkedHashMap<>();
		contents.put(EmbeddingTestHelper.randomNodeId(), "Testing distance normalization values");
		this.repository.populateFts(contents);

		List<SearchResult> results = this.repository.searchKeyword("testing", 10);
		assertFalse(results.isEmpty());
		for (SearchResult result : results) {
			assertTrue(result.getDistance() >= 0.0);
			assertTrue(result.getDistance() < 1.0);
		}
	}

	@Test
	void getContentHash_withNoHash_returnsNull() throws SQLException {
		String hash = this.repository.getContentHash("nonexistent", "minilm");
		assertNull(hash);
	}

	@Test
	void saveAndGetContentHash_roundTrips() throws SQLException {
		String nodeId = EmbeddingTestHelper.randomNodeId();
		this.repository.saveContentHash(nodeId, "minilm", "abc123");

		String hash = this.repository.getContentHash(nodeId, "minilm");
		assertEquals("abc123", hash);
	}

	@Test
	void saveContentHash_updatesExisting() throws SQLException {
		String nodeId = EmbeddingTestHelper.randomNodeId();
		this.repository.saveContentHash(nodeId, "minilm", "hash1");
		this.repository.saveContentHash(nodeId, "minilm", "hash2");

		String hash = this.repository.getContentHash(nodeId, "minilm");
		assertEquals("hash2", hash);
	}

	@Test
	void getContentHash_filtersByModel() throws SQLException {
		String nodeId = EmbeddingTestHelper.randomNodeId();
		this.repository.saveContentHash(nodeId, "minilm", "hash-minilm");
		this.repository.saveContentHash(nodeId, "mpnet", "hash-mpnet");

		assertEquals("hash-minilm", this.repository.getContentHash(nodeId, "minilm"));
		assertEquals("hash-mpnet", this.repository.getContentHash(nodeId, "mpnet"));
	}

	@Test
	void searchKeyword_usesPorterStemming() throws SQLException {
		String nodeId = EmbeddingTestHelper.randomNodeId();
		Map<String, String> contents = new LinkedHashMap<>();
		contents.put(nodeId, "The programmers were programming their programs");
		this.repository.populateFts(contents);

		List<SearchResult> results = this.repository.searchKeyword("program", 10);
		assertEquals(1, results.size());
		assertEquals(nodeId, results.getFirst().getNodeId());
	}

	@Test
	void search_withThreshold_filtersResults() throws SQLException {
		if (!this.connection.isSqliteVecLoaded()) {
			return;
		}

		float[] baseEmbedding = EmbeddingTestHelper.createRandomEmbedding(EmbeddingModel.MINILM.getDimensions());

		String closeNodeId = EmbeddingTestHelper.randomNodeId();
		float[] closeEmbedding = EmbeddingTestHelper.createSimilarEmbedding(baseEmbedding, 0.05f);
		this.repository.save(
			EmbeddingTestHelper.createNodeEmbedding(closeNodeId, EmbeddingModel.MINILM, closeEmbedding)
		);

		String farNodeId = EmbeddingTestHelper.randomNodeId();
		float[] farEmbedding = EmbeddingTestHelper.createRandomEmbedding(EmbeddingModel.MINILM.getDimensions());
		this.repository.save(EmbeddingTestHelper.createNodeEmbedding(farNodeId, EmbeddingModel.MINILM, farEmbedding));

		List<SearchResult> results = this.repository.search(baseEmbedding, EmbeddingModel.MINILM, 10, 0.2);

		assertFalse(results.isEmpty());
		for (SearchResult result : results) {
			assertTrue(result.getDistance() < 0.2);
		}
	}
}
