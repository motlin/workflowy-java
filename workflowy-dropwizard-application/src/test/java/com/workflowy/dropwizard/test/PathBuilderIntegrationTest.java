package com.workflowy.dropwizard.test;

import com.workflowy.embedding.generator.PathBuilder;
import io.liftwizard.reladomo.test.extension.ReladomoTestFile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PathBuilderIntegrationTest extends AbstractWorkflowyAppTest
{
    private final PathBuilder pathBuilder = new PathBuilder();

    @Test
    @ReladomoTestFile("test-data/basic-hierarchy.txt")
    void buildFullPath_forRootNode_returnsNodeName()
    {
        String path = this.pathBuilder.buildFullPath("00000000-0000-0000-0000-000000000001");

        assertEquals("Root Node", path);
    }

    @Test
    @ReladomoTestFile("test-data/basic-hierarchy.txt")
    void buildFullPath_forChildNode_returnsParentAndChildPath()
    {
        String path = this.pathBuilder.buildFullPath("00000000-0000-0000-0000-000000000002");

        assertEquals("Root Node > Child 1", path);
    }

    @Test
    @ReladomoTestFile("test-data/basic-hierarchy.txt")
    void buildFullPath_forGrandchildNode_returnsFullPath()
    {
        String path = this.pathBuilder.buildFullPath("00000000-0000-0000-0000-000000000004");

        assertEquals("Root Node > Child 1 > Grandchild 1", path);
    }

    @Test
    @ReladomoTestFile("test-data/basic-hierarchy.txt")
    void buildFullPath_forNonExistentNode_returnsEmptyString()
    {
        String path = this.pathBuilder.buildFullPath("00000000-0000-0000-0000-nonexistent");

        assertEquals("", path);
    }

    @Test
    @ReladomoTestFile("test-data/basic-hierarchy.txt")
    void buildTextContent_forNodeWithNote_returnsNameAndNote()
    {
        String content = this.pathBuilder.buildTextContent("00000000-0000-0000-0000-000000000001");

        assertEquals("Root Node\n\nThis is the root", content);
    }

    @Test
    @ReladomoTestFile("test-data/basic-hierarchy.txt")
    void buildTextContent_forNodeWithoutNote_returnsNameOnly()
    {
        String content = this.pathBuilder.buildTextContent("00000000-0000-0000-0000-000000000003");

        assertEquals("Child 2", content);
    }

    @Test
    @ReladomoTestFile("test-data/basic-hierarchy.txt")
    void buildTextContent_forNonExistentNode_returnsEmptyString()
    {
        String content = this.pathBuilder.buildTextContent("00000000-0000-0000-0000-nonexistent");

        assertEquals("", content);
    }

    @Test
    @ReladomoTestFile("test-data/basic-hierarchy.txt")
    void buildEmbeddingText_forChildWithNote_returnsPathAndContent()
    {
        String embeddingText = this.pathBuilder.buildEmbeddingText("00000000-0000-0000-0000-000000000002");

        assertEquals("Root Node > Child 1\n\nChild 1\n\nFirst child note", embeddingText);
    }

    @Test
    @ReladomoTestFile("test-data/basic-hierarchy.txt")
    void buildEmbeddingText_forRootWithNote_returnsNodeAndContent()
    {
        String embeddingText = this.pathBuilder.buildEmbeddingText("00000000-0000-0000-0000-000000000001");

        assertEquals("Root Node\n\nRoot Node\n\nThis is the root", embeddingText);
    }

    @Test
    @ReladomoTestFile("test-data/basic-hierarchy.txt")
    void buildEmbeddingText_forNonExistentNode_returnsEmptyString()
    {
        String embeddingText = this.pathBuilder.buildEmbeddingText("00000000-0000-0000-0000-nonexistent");

        assertEquals("", embeddingText);
    }
}
