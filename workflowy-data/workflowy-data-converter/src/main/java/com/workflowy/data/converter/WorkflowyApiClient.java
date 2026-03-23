package com.workflowy.data.converter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflowy.data.pojo.ApiInputItem;
import com.workflowy.data.pojo.ApiResponse;
import com.workflowy.data.pojo.CreateNodeRequest;
import com.workflowy.data.pojo.CreateNodeResponse;
import com.workflowy.data.pojo.GetNodeResponse;
import com.workflowy.data.pojo.MoveNodeRequest;
import com.workflowy.data.pojo.UpdateNodeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowyApiClient implements AutoCloseable {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowyApiClient.class);

	private static final String DEFAULT_BASE_URL = "https://workflowy.com";

	private final String baseUrl;
	private final String apiKey;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	public WorkflowyApiClient(@Nonnull String apiKey) {
		this(apiKey, DEFAULT_BASE_URL);
	}

	public WorkflowyApiClient(@Nonnull String apiKey, @Nonnull String baseUrl) {
		this.apiKey = apiKey;
		this.baseUrl = baseUrl;
		this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
		this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	public ApiInputItem getNode(@Nonnull String nodeId) {
		GetNodeResponse response = this.sendGet("/api/v1/nodes/" + nodeId, GetNodeResponse.class);
		return response.node();
	}

	public List<ApiInputItem> getRootNodes() {
		ApiResponse response = this.sendGet("/api/v1/nodes", ApiResponse.class);
		return response.getNodesOrEmpty();
	}

	public List<ApiInputItem> getChildNodes(@Nonnull String parentId) {
		ApiResponse response = this.sendGet("/api/v1/nodes?parent_id=" + parentId, ApiResponse.class);
		return response.getNodesOrEmpty();
	}

	public List<ApiInputItem> exportAllNodes() {
		ApiResponse response = this.sendGet("/api/v1/nodes-export", ApiResponse.class);
		return response.getNodesOrEmpty();
	}

	public String createNode(@Nonnull CreateNodeRequest request) {
		CreateNodeResponse response = this.sendPost("/api/v1/nodes/", request, CreateNodeResponse.class);
		return response.itemId();
	}

	public void updateNode(@Nonnull String nodeId, @Nonnull UpdateNodeRequest request) {
		this.sendPost("/api/v1/nodes/" + nodeId, request, String.class);
	}

	public void deleteNode(@Nonnull String nodeId) {
		this.sendDelete("/api/v1/nodes/" + nodeId);
	}

	public void completeNode(@Nonnull String nodeId) {
		this.sendPost("/api/v1/nodes/" + nodeId + "/complete", null, String.class);
	}

	public void uncompleteNode(@Nonnull String nodeId) {
		this.sendPost("/api/v1/nodes/" + nodeId + "/uncomplete", null, String.class);
	}

	public void moveNode(@Nonnull String nodeId, @Nonnull String newParentId, @Nullable String position) {
		var request = new MoveNodeRequest(newParentId, position);
		this.sendPost("/api/v1/nodes/" + nodeId + "/move", request, String.class);
	}

	@Nullable
	public ApiInputItem findNodeByPath(@Nonnull List<String> pathSegments) {
		return this.findNodeByPath(pathSegments, null);
	}

	@Nullable
	public ApiInputItem findNodeByPath(@Nonnull List<String> pathSegments, @Nullable String parentId) {
		if (pathSegments.isEmpty()) {
			return null;
		}

		List<ApiInputItem> currentChildren = parentId != null ? this.getChildNodes(parentId) : this.getRootNodes();

		ApiInputItem current = null;

		for (String segment : pathSegments) {
			String trimmed = segment.trim();
			current = null;

			for (ApiInputItem child : currentChildren) {
				if (trimmed.equals(child.name())) {
					current = child;
					break;
				}
			}

			if (current == null) {
				return null;
			}

			currentChildren = this.getChildNodes(current.id());
		}

		return current;
	}

	private <T> T sendGet(String path, Class<T> responseType) {
		try {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(this.baseUrl + path))
				.header("Authorization", "Bearer " + this.apiKey)
				.header("Accept", "application/json")
				.GET()
				.timeout(Duration.ofSeconds(60))
				.build();

			HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				throw new WorkflowyApiException(
					"GET " + path + " failed: " + response.statusCode() + " " + response.body()
				);
			}

			return this.objectMapper.readValue(response.body(), responseType);
		} catch (IOException | InterruptedException e) {
			throw new WorkflowyApiException("GET " + path + " failed", e);
		}
	}

	private <T> T sendPost(String path, @Nullable Object body, Class<T> responseType) {
		try {
			HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create(this.baseUrl + path))
				.header("Authorization", "Bearer " + this.apiKey)
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.timeout(Duration.ofSeconds(60));

			if (body != null) {
				builder.POST(HttpRequest.BodyPublishers.ofString(this.objectMapper.writeValueAsString(body)));
			} else {
				builder.POST(HttpRequest.BodyPublishers.noBody());
			}

			HttpResponse<String> response = this.httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				throw new WorkflowyApiException(
					"POST " + path + " failed: " + response.statusCode() + " " + response.body()
				);
			}

			if (responseType == String.class) {
				@SuppressWarnings("unchecked")
				T result = (T) response.body();
				return result;
			}

			return this.objectMapper.readValue(response.body(), responseType);
		} catch (IOException | InterruptedException e) {
			throw new WorkflowyApiException("POST " + path + " failed", e);
		}
	}

	private void sendDelete(String path) {
		try {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(this.baseUrl + path))
				.header("Authorization", "Bearer " + this.apiKey)
				.DELETE()
				.timeout(Duration.ofSeconds(60))
				.build();

			HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				throw new WorkflowyApiException(
					"DELETE " + path + " failed: " + response.statusCode() + " " + response.body()
				);
			}
		} catch (IOException | InterruptedException e) {
			throw new WorkflowyApiException("DELETE " + path + " failed", e);
		}
	}

	@Override
	public void close() {}
}
