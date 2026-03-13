package com.workflowy.embedding.repository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqliteVecConnection implements AutoCloseable {

	private static final Logger LOGGER = LoggerFactory.getLogger(SqliteVecConnection.class);

	private static final String CREATE_TABLE_SQL = """
		CREATE TABLE IF NOT EXISTS node_embeddings (
		    node_id TEXT NOT NULL,
		    model TEXT NOT NULL DEFAULT 'minilm',
		    embedding BLOB NOT NULL,
		    system_from TEXT NOT NULL,
		    system_to TEXT NOT NULL,
		    PRIMARY KEY (node_id, model, system_from)
		)
		""";

	private static final String CREATE_INDEX_SQL = """
		CREATE INDEX IF NOT EXISTS idx_node_embeddings_model_system_to
		ON node_embeddings (model, system_to)
		""";

	private static final String CREATE_FTS_TABLE_SQL = """
		CREATE VIRTUAL TABLE IF NOT EXISTS node_fts USING fts5(
		    node_id UNINDEXED,
		    content,
		    tokenize='porter unicode61'
		)
		""";

	private static final String CREATE_CONTENT_HASH_TABLE_SQL = """
		CREATE TABLE IF NOT EXISTS node_content_hash (
		    node_id TEXT NOT NULL,
		    model TEXT NOT NULL,
		    content_hash TEXT NOT NULL,
		    PRIMARY KEY (node_id, model)
		)
		""";

	private final Connection connection;
	private final String databasePath;
	private boolean sqliteVecLoaded;

	public SqliteVecConnection(String databasePath) throws SQLException {
		this.databasePath = databasePath;
		this.connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath + "?enable_load_extension=true");

		this.initializeDatabase();
	}

	private void initializeDatabase() throws SQLException {
		this.loadSqliteVecExtension();

		try (Statement stmt = this.connection.createStatement()) {
			stmt.execute(CREATE_TABLE_SQL);
			stmt.execute(CREATE_INDEX_SQL);
			stmt.execute(CREATE_FTS_TABLE_SQL);
			stmt.execute(CREATE_CONTENT_HASH_TABLE_SQL);
		}

		LOGGER.info("SQLite database initialized at: {}", this.databasePath);
	}

	private void loadSqliteVecExtension() {
		try {
			Path extensionPath = this.extractNativeLibrary();
			if (extensionPath != null) {
				try (Statement stmt = this.connection.createStatement()) {
					stmt.execute("SELECT load_extension('" + extensionPath.toString().replace("'", "''") + "')");
					this.sqliteVecLoaded = true;
					LOGGER.info("sqlite-vec extension loaded successfully");
				}
			}
		} catch (SQLException | IOException e) {
			LOGGER.warn("Failed to load sqlite-vec extension. Vector search will not be available: {}", e.getMessage());
		}
	}

	private Path extractNativeLibrary() throws IOException {
		String osName = System.getProperty("os.name").toLowerCase();
		String osArch = System.getProperty("os.arch").toLowerCase();

		String platformDir;
		String libraryName;
		if (osName.contains("mac")) {
			libraryName = "vec0.dylib";
			platformDir = (osArch.contains("aarch64") || osArch.contains("arm")) ? "macos-aarch64" : "macos-x86_64";
		} else if (osName.contains("linux")) {
			libraryName = "vec0.so";
			platformDir = (osArch.contains("aarch64") || osArch.contains("arm")) ? "linux-aarch64" : "linux-x86_64";
		} else if (osName.contains("windows")) {
			libraryName = "vec0.dll";
			platformDir = "windows-x86_64";
		} else {
			LOGGER.warn("Unsupported OS for sqlite-vec: {}", osName);
			return null;
		}

		String resourcePath = "/sqlite-vec/" + platformDir + "/" + libraryName;
		try (InputStream is = this.getClass().getResourceAsStream(resourcePath)) {
			if (is == null) {
				LOGGER.warn("sqlite-vec native library not found in resources: {}", resourcePath);
				return null;
			}

			Path tempDir = Files.createTempDirectory("sqlite-vec");
			Path tempFile = tempDir.resolve(libraryName);
			Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
			tempFile.toFile().deleteOnExit();
			tempDir.toFile().deleteOnExit();

			return tempFile;
		}
	}

	public Connection getConnection() {
		return this.connection;
	}

	public boolean isSqliteVecLoaded() {
		return this.sqliteVecLoaded;
	}

	@Override
	public void close() throws SQLException {
		if (this.connection != null && !this.connection.isClosed()) {
			this.connection.close();
		}
	}
}
