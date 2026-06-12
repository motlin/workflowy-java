package com.workflowy.embedding.engine;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.workflowy.embedding.model.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HuggingFaceModelDownloader {

	private static final Logger LOGGER = LoggerFactory.getLogger(HuggingFaceModelDownloader.class);

	private static final String HUGGINGFACE_BASE_URL = "https://huggingface.co";
	private static final String MODEL_FILENAME = "model.onnx";
	private static final String TOKENIZER_FILENAME = "tokenizer.json";

	private HuggingFaceModelDownloader() {
		throw new AssertionError("Suppress default constructor for noninstantiability");
	}

	public static Path ensureModelDownloaded(EmbeddingModel model, String cachePath) throws IOException {
		Path modelDir = Path.of(cachePath, model.getKey());
		Path modelFile = modelDir.resolve(MODEL_FILENAME);
		Path tokenizerFile = modelDir.resolve(TOKENIZER_FILENAME);

		if (Files.exists(modelFile) && Files.exists(tokenizerFile)) {
			LOGGER.info("Model {} already downloaded at {}", model.getKey(), modelDir);
			return modelDir;
		}

		Files.createDirectories(modelDir);

		String modelName = model.getModelName();

		if (!Files.exists(modelFile)) {
			String modelUrl = buildModelUrl(modelName);
			LOGGER.info("Downloading ONNX model from {}", modelUrl);
			downloadFile(modelUrl, modelFile);
			LOGGER.info("Downloaded model to {}", modelFile);
		}

		if (!Files.exists(tokenizerFile)) {
			String tokenizerUrl = buildTokenizerUrl(modelName);
			LOGGER.info("Downloading tokenizer from {}", tokenizerUrl);
			downloadFile(tokenizerUrl, tokenizerFile);
			LOGGER.info("Downloaded tokenizer to {}", tokenizerFile);
		}

		return modelDir;
	}

	private static String buildModelUrl(String modelName) {
		return HUGGINGFACE_BASE_URL + "/" + modelName + "/resolve/main/onnx/" + MODEL_FILENAME;
	}

	private static String buildTokenizerUrl(String modelName) {
		return HUGGINGFACE_BASE_URL + "/" + modelName + "/resolve/main/" + TOKENIZER_FILENAME;
	}

	private static void downloadFile(String urlString, Path destination) throws IOException {
		URI uri = URI.create(urlString);
		var connection = (HttpURLConnection) uri.toURL().openConnection();
		connection.setRequestProperty("User-Agent", "workflowy-java/1.0");
		connection.setConnectTimeout(30000);
		connection.setReadTimeout(300000);

		int responseCode = connection.getResponseCode();
		if (
			responseCode == HttpURLConnection.HTTP_MOVED_TEMP
			|| responseCode == HttpURLConnection.HTTP_MOVED_PERM
			|| responseCode == 307
			|| responseCode == 308
		) {
			String newUrl = connection.getHeaderField("Location");
			LOGGER.debug("Following redirect to {}", newUrl);
			connection.disconnect();
			downloadFile(newUrl, destination);
			return;
		}

		if (responseCode != HttpURLConnection.HTTP_OK) {
			connection.disconnect();
			throw new IOException("Failed to download " + urlString + ": HTTP " + responseCode);
		}

		long contentLength = connection.getContentLengthLong();
		LOGGER.info("Downloading {} bytes...", contentLength > 0 ? contentLength : "unknown");

		try (InputStream in = connection.getInputStream()) {
			Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
		} finally {
			connection.disconnect();
		}
	}

	public static Path getModelPath(EmbeddingModel model, String cachePath) {
		return Path.of(cachePath, model.getKey());
	}

	public static boolean isModelDownloaded(EmbeddingModel model, String cachePath) {
		Path modelDir = getModelPath(model, cachePath);
		Path modelFile = modelDir.resolve(MODEL_FILENAME);
		Path tokenizerFile = modelDir.resolve(TOKENIZER_FILENAME);
		return Files.exists(modelFile) && Files.exists(tokenizerFile);
	}
}
