package com.workflowy.dropwizard.test;

import io.liftwizard.reladomo.test.extension.ReladomoTestFile;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Test;

class NodeContentResourceTest extends AbstractWorkflowyAppTest
{
    @Test
    @ReladomoTestFile("test-data/basic-hierarchy.txt")
    void getNode_withExistingId_returnsNode()
    {
        Client client = this.getClient("getNode_withExistingId_returnsNode");

        Response response = client
                .target("http://localhost:{port}/api/node/{id}")
                .resolveTemplate("port", this.appExtension.getLocalPort())
                .resolveTemplate("id", "00000000-0000-0000-0000-000000000001")
                .request()
                .get();

        this.assertResponseStatus(response, Status.OK);
    }

    @Test
    @ReladomoTestFile("test-data/basic-hierarchy.txt")
    void getNode_withNonExistentId_returnsGone()
    {
        Client client = this.getClient("getNode_withNonExistentId_returnsGone");

        Response response = client
                .target("http://localhost:{port}/api/node/{id}")
                .resolveTemplate("port", this.appExtension.getLocalPort())
                .resolveTemplate("id", "00000000-0000-0000-0000-nonexistent")
                .request()
                .get();

        this.assertResponseStatus(response, Status.GONE);
    }

    @Test
    @ReladomoTestFile("test-data/basic-hierarchy.txt")
    void getChildren_withExistingParent_returnsChildren()
    {
        Client client = this.getClient("getChildren_withExistingParent_returnsChildren");

        Response response = client
                .target("http://localhost:{port}/api/children/{parentId}")
                .resolveTemplate("port", this.appExtension.getLocalPort())
                .resolveTemplate("parentId", "00000000-0000-0000-0000-000000000001")
                .request()
                .get();

        this.assertResponseStatus(response, Status.OK);
    }

    @Test
    @ReladomoTestFile("test-data/basic-hierarchy.txt")
    void getRootNodes_returnsRootNodes()
    {
        Client client = this.getClient("getRootNodes_returnsRootNodes");

        Response response = client
                .target("http://localhost:{port}/api/nodes/root")
                .resolveTemplate("port", this.appExtension.getLocalPort())
                .request()
                .get();

        this.assertResponseStatus(response, Status.OK);
    }

    @Test
    @ReladomoTestFile("test-data/basic-hierarchy.txt")
    void searchNodes_withMatchingQuery_returnsResults()
    {
        Client client = this.getClient("searchNodes_withMatchingQuery_returnsResults");

        Response response = client
                .target("http://localhost:{port}/api/nodes/search")
                .resolveTemplate("port", this.appExtension.getLocalPort())
                .queryParam("query", "Child")
                .request()
                .get();

        this.assertResponseStatus(response, Status.OK);
    }
}
