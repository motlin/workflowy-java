package com.workflowy.dropwizard.test;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwaggerSpecResourceTest
	extends AbstractWorkflowyAppTest
{
	@Test
	void swaggerSpec_isServed()
		throws Exception
	{
		Client client = this.getClient("swaggerSpec_isServed");

		Response response = client
			.target("http://localhost:{port}/swagger.json")
			.resolveTemplate("port", this.appExtension.getLocalPort())
			.request(MediaType.APPLICATION_JSON_TYPE)
			.get();

		this.assertResponseStatus(response, Status.OK);

		JsonNode swaggerSpec = new ObjectMapper().readTree(response.readEntity(String.class));
		assertEquals("2.0", swaggerSpec.path("swagger").asText());
		assertTrue(swaggerSpec.path("paths").has("/api/node/{id}"), swaggerSpec.path("paths").toString());
	}
}
