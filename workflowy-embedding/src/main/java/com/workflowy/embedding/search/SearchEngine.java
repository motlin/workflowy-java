package com.workflowy.embedding.search;

import java.sql.SQLException;
import java.util.List;

import com.workflowy.NodeContent;
import com.workflowy.NodeContentFinder;
import com.workflowy.embedding.engine.EmbeddingEngine;
import com.workflowy.embedding.generator.PathBuilder;
import com.workflowy.embedding.model.EmbeddingModel;
import com.workflowy.embedding.repository.EmbeddingRepository;
import com.workflowy.embedding.util.HtmlStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchEngine
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchEngine.class);

    private final EmbeddingEngine engine;
    private final EmbeddingRepository repository;
    private final PathBuilder pathBuilder;

    public SearchEngine(EmbeddingEngine engine, EmbeddingRepository repository)
    {
        this.engine = engine;
        this.repository = repository;
        this.pathBuilder = new PathBuilder();
    }

    public List<SearchResult> search(String query, int limit, Double threshold) throws SQLException
    {
        EmbeddingModel model = this.engine.getModel();

        double effectiveThreshold = threshold != null ? threshold : model.getDefaultThreshold();

        float[] queryEmbedding = this.engine.generateEmbedding(query, true);

        List<SearchResult> results = this.repository.search(
                queryEmbedding,
                model,
                limit,
                effectiveThreshold);

        for (SearchResult result : results)
        {
            this.enrichResult(result);
        }

        return results;
    }

    private void enrichResult(SearchResult result)
    {
        NodeContent node = NodeContentFinder.findOne(NodeContentFinder.id().eq(result.getNodeId()));

        if (node != null)
        {
            result.setName(HtmlStripper.stripHtmlTags(node.getName()));
            result.setNote(node.getNote() != null ? HtmlStripper.stripHtmlTags(node.getNote()) : null);
            result.setFullPath(this.pathBuilder.buildFullPath(result.getNodeId()));
            result.setTextContent(this.pathBuilder.buildTextContent(result.getNodeId()));
        }
    }
}
