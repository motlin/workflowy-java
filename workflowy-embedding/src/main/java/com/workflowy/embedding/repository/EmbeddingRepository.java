package com.workflowy.embedding.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import com.workflowy.embedding.model.EmbeddingModel;
import com.workflowy.embedding.model.NodeEmbedding;
import com.workflowy.embedding.search.SearchResult;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddingRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingRepository.class);

	private static final String FAR_FUTURE_DATE = "9999-12-31 23:59:59";
	private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final String INSERT_SQL = """
		INSERT INTO node_embeddings (node_id, model, embedding, system_from, system_to)
		VALUES (?, ?, ?, ?, ?)
		""";

	private static final String UPDATE_SYSTEM_TO_SQL = """
		UPDATE node_embeddings
		SET system_to = ?
		WHERE node_id = ? AND model = ? AND system_to = ?
		""";

	private static final String GET_EXISTING_IDS_SQL = """
		SELECT node_id FROM node_embeddings
		WHERE model = ? AND system_to = ?
		""";

	private static final String SEARCH_SQL = """
		SELECT node_id, embedding,
		       vec_distance_cosine(embedding, ?) as distance
		FROM node_embeddings
		WHERE model = ? AND system_to = ?
		ORDER BY distance ASC
		LIMIT ?
		""";

	private static final String SEARCH_WITH_THRESHOLD_SQL = """
		SELECT node_id, embedding,
		       vec_distance_cosine(embedding, ?) as distance
		FROM node_embeddings
		WHERE model = ? AND system_to = ?
		AND vec_distance_cosine(embedding, ?) < ?
		ORDER BY distance ASC
		LIMIT ?
		""";

	private final SqliteVecConnection sqliteVecConnection;

	public EmbeddingRepository(SqliteVecConnection sqliteVecConnection) {
		this.sqliteVecConnection = sqliteVecConnection;
	}

	public void save(NodeEmbedding embedding) throws SQLException {
		Connection conn = this.sqliteVecConnection.getConnection();
		String now = DATETIME_FORMATTER.format(Instant.now().atZone(ZoneOffset.UTC));

		try (PreparedStatement updateStmt = conn.prepareStatement(UPDATE_SYSTEM_TO_SQL)) {
			updateStmt.setString(1, now);
			updateStmt.setString(2, embedding.getNodeId());
			updateStmt.setString(3, embedding.getModel());
			updateStmt.setString(4, FAR_FUTURE_DATE);
			updateStmt.executeUpdate();
		}

		try (PreparedStatement insertStmt = conn.prepareStatement(INSERT_SQL)) {
			insertStmt.setString(1, embedding.getNodeId());
			insertStmt.setString(2, embedding.getModel());
			insertStmt.setBytes(3, embedding.getEmbeddingAsBytes());
			insertStmt.setString(4, now);
			insertStmt.setString(5, FAR_FUTURE_DATE);
			insertStmt.executeUpdate();
		}
	}

	public void saveBatch(List<NodeEmbedding> embeddings) throws SQLException {
		Connection conn = this.sqliteVecConnection.getConnection();
		String now = DATETIME_FORMATTER.format(Instant.now().atZone(ZoneOffset.UTC));

		conn.setAutoCommit(false);
		try {
			try (PreparedStatement updateStmt = conn.prepareStatement(UPDATE_SYSTEM_TO_SQL)) {
				for (NodeEmbedding embedding : embeddings) {
					updateStmt.setString(1, now);
					updateStmt.setString(2, embedding.getNodeId());
					updateStmt.setString(3, embedding.getModel());
					updateStmt.setString(4, FAR_FUTURE_DATE);
					updateStmt.addBatch();
				}
				updateStmt.executeBatch();
			}

			try (PreparedStatement insertStmt = conn.prepareStatement(INSERT_SQL)) {
				for (NodeEmbedding embedding : embeddings) {
					insertStmt.setString(1, embedding.getNodeId());
					insertStmt.setString(2, embedding.getModel());
					insertStmt.setBytes(3, embedding.getEmbeddingAsBytes());
					insertStmt.setString(4, now);
					insertStmt.setString(5, FAR_FUTURE_DATE);
					insertStmt.addBatch();
				}
				insertStmt.executeBatch();
			}

			conn.commit();
		} catch (SQLException e) {
			conn.rollback();
			throw e;
		} finally {
			conn.setAutoCommit(true);
		}
	}

	public Set<String> getExistingNodeIds(EmbeddingModel model) throws SQLException {
		MutableSet<String> existingIds = Sets.mutable.empty();
		Connection conn = this.sqliteVecConnection.getConnection();

		try (PreparedStatement stmt = conn.prepareStatement(GET_EXISTING_IDS_SQL)) {
			stmt.setString(1, model.getKey());
			stmt.setString(2, FAR_FUTURE_DATE);

			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					existingIds.add(rs.getString("node_id"));
				}
			}
		}

		return existingIds;
	}

	public List<SearchResult> search(float[] queryEmbedding, EmbeddingModel model, int limit, Double threshold)
		throws SQLException {
		if (!this.sqliteVecConnection.isSqliteVecLoaded()) {
			throw new SQLException("sqlite-vec extension not loaded. Vector search is not available.");
		}

		MutableList<SearchResult> results = Lists.mutable.empty();
		Connection conn = this.sqliteVecConnection.getConnection();

		byte[] queryBytes = floatArrayToBytes(queryEmbedding);

		String sql = threshold != null ? SEARCH_WITH_THRESHOLD_SQL : SEARCH_SQL;

		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			int paramIndex = 1;
			stmt.setBytes(paramIndex++, queryBytes);
			stmt.setString(paramIndex++, model.getKey());
			stmt.setString(paramIndex++, FAR_FUTURE_DATE);

			if (threshold != null) {
				stmt.setBytes(paramIndex++, queryBytes);
				stmt.setDouble(paramIndex++, threshold);
			}

			stmt.setInt(paramIndex, limit);

			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					String nodeId = rs.getString("node_id");
					double distance = rs.getDouble("distance");

					SearchResult result = new SearchResult(nodeId, distance);
					results.add(result);
				}
			}
		}

		return results;
	}

	private static byte[] floatArrayToBytes(float[] floats) {
		byte[] bytes = new byte[floats.length * 4];
		for (int i = 0; i < floats.length; i++) {
			int intBits = Float.floatToIntBits(floats[i]);
			bytes[i * 4] = (byte) (intBits & 0xFF);
			bytes[i * 4 + 1] = (byte) ((intBits >> 8) & 0xFF);
			bytes[i * 4 + 2] = (byte) ((intBits >> 16) & 0xFF);
			bytes[i * 4 + 3] = (byte) ((intBits >> 24) & 0xFF);
		}
		return bytes;
	}
}
