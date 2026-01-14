package com.workflowy.embedding.engine;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.workflowy.embedding.model.EmbeddingModel;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenAIEmbeddingEngine implements EmbeddingEngine {

	private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIEmbeddingEngine.class);

	private static final String OPENAI_EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";

	private final EmbeddingModel model;
	private final String apiKey;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	public OpenAIEmbeddingEngine(EmbeddingModel model, String apiKey) {
		if (!model.isOpenAI()) {
			throw new IllegalArgumentException("Model must be an OpenAI model: " + model);
		}
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalArgumentException("OpenAI API key is required");
		}

		this.model = model;
		this.apiKey = apiKey;
		this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public float[] generateEmbedding(String text, boolean isQuery) {
		String prefixedText = this.applyPrefix(text, isQuery);

		try {
			ObjectNode requestBody = this.objectMapper.createObjectNode();
			requestBody.put("input", prefixedText);
			requestBody.put("model", this.model.getModelName());

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(OPENAI_EMBEDDINGS_URL))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + this.apiKey)
				.POST(HttpRequest.BodyPublishers.ofString(this.objectMapper.writeValueAsString(requestBody)))
				.timeout(Duration.ofSeconds(60))
				.build();

			HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				throw new RuntimeException("OpenAI API error: " + response.statusCode() + " " + response.body());
			}

			JsonNode responseJson = this.objectMapper.readTree(response.body());
			JsonNode embeddingArray = responseJson.get("data").get(0).get("embedding");

			float[] embedding = new float[embeddingArray.size()];
			for (int i = 0; i < embeddingArray.size(); i++) {
				embedding[i] = (float) embeddingArray.get(i).asDouble();
			}

			return embedding;
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Failed to generate embedding", e);
		}
	}

	@Override
	public List<float[]> generateEmbeddings(List<String> texts, boolean isQuery) {
		List<String> prefixedTexts = texts
			.stream()
			.map((text) -> this.applyPrefix(text, isQuery))
			.toList();

		try {
			ObjectNode requestBody = this.objectMapper.createObjectNode();
			ArrayNode inputArray = requestBody.putArray("input");
			for (String text : prefixedTexts) {
				inputArray.add(text);
			}
			requestBody.put("model", this.model.getModelName());

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(OPENAI_EMBEDDINGS_URL))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + this.apiKey)
				.POST(HttpRequest.BodyPublishers.ofString(this.objectMapper.writeValueAsString(requestBody)))
				.timeout(Duration.ofSeconds(120))
				.build();

			HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				throw new RuntimeException("OpenAI API error: " + response.statusCode() + " " + response.body());
			}

			JsonNode responseJson = this.objectMapper.readTree(response.body());
			JsonNode dataArray = responseJson.get("data");

			MutableList<float[]> embeddings = Lists.mutable.empty();
			for (JsonNode item : dataArray) {
				JsonNode embeddingArray = item.get("embedding");
				float[] embedding = new float[embeddingArray.size()];
				for (int i = 0; i < embeddingArray.size(); i++) {
					embedding[i] = (float) embeddingArray.get(i).asDouble();
				}
				embeddings.add(embedding);
			}

			return embeddings;
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Failed to generate embeddings", e);
		}
	}

	private String applyPrefix(String text, boolean isQuery) {
		String prefix = isQuery ? this.model.getQueryPrefix().orElse("") : this.model.getPassagePrefix().orElse("");
		return prefix + text;
	}

	@Override
	public EmbeddingModel getModel() {
		return this.model;
	}

	@Override
	public void close() {}
}
