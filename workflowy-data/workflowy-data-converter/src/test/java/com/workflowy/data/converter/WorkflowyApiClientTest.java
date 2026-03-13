package com.workflowy.data.converter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkflowyApiClientTest {

	@Test
	void constructor_withValidApiKey_succeeds() {
		try (var client = new WorkflowyApiClient("test-api-key")) {
			assertNotNull(client);
		}
	}

	@Test
	void constructor_withCustomBaseUrl_succeeds() {
		try (var client = new WorkflowyApiClient("test-api-key", "https://custom.example.com")) {
			assertNotNull(client);
		}
	}

	@Test
	void getNode_withInvalidUrl_throwsApiException() {
		try (var client = new WorkflowyApiClient("test-key", "http://localhost:1")) {
			assertThrows(WorkflowyApiException.class, () -> client.getNode("fake-id"));
		}
	}
}
