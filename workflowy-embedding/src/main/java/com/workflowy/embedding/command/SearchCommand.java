package com.workflowy.embedding.command;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflowy.embedding.config.EmbeddingConfiguration;
import com.workflowy.embedding.config.EmbeddingConfigurationProvider;
import com.workflowy.embedding.engine.EmbeddingEngine;
import com.workflowy.embedding.engine.EmbeddingEngineFactory;
import com.workflowy.embedding.model.EmbeddingModel;
import com.workflowy.embedding.repository.EmbeddingRepository;
import com.workflowy.embedding.repository.SqliteVecConnection;
import com.workflowy.embedding.search.SearchEngine;
import com.workflowy.embedding.search.SearchResult;
import cool.klass.dropwizard.configuration.AbstractKlassConfiguration;
import io.dropwizard.Application;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.thread.ShutdownThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchCommand<T extends AbstractKlassConfiguration & EmbeddingConfigurationProvider>
        extends EnvironmentCommand<T>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchCommand.class);

    private final ContainerLifeCycle containerLifeCycle = new ContainerLifeCycle();

    public SearchCommand(Application<T> application)
    {
        super(application, "search", "Search nodes using semantic similarity");
    }

    @Override
    public void configure(Subparser subparser)
    {
        super.configure(subparser);

        subparser.addArgument("query")
                .type(String.class)
                .required(true)
                .help("Search query text");

        subparser.addArgument("--model")
                .type(String.class)
                .setDefault("minilm")
                .help("Embedding model to use: minilm, mpnet, bge, openai-small, openai-large");

        subparser.addArgument("--limit")
                .type(Integer.class)
                .setDefault(5)
                .help("Maximum number of results to return");

        subparser.addArgument("--threshold")
                .type(Double.class)
                .help("Similarity threshold (0-1). Lower = more similar. Default depends on model.");

        subparser.addArgument("--db-path")
                .type(String.class)
                .help("Override path to the embeddings SQLite database");
    }

    @Override
    protected void run(
            @Nonnull Environment environment,
            Namespace namespace,
            @Nonnull T configuration)
            throws Exception
    {
        LOGGER.info("Running {}.", this.getClass().getSimpleName());

        environment.lifecycle().getManagedObjects().forEach(this.containerLifeCycle::addBean);
        ShutdownThread.register(this.containerLifeCycle);
        this.containerLifeCycle.start();

        EmbeddingConfiguration embeddingConfig = configuration.getEmbeddingConfiguration();
        ObjectMapper objectMapper = environment.getObjectMapper();

        String query = namespace.getString("query");
        String modelKey = namespace.getString("model");
        int limit = namespace.getInt("limit");
        Double threshold = namespace.getDouble("threshold");
        String dbPath = namespace.getString("db_path");

        if (dbPath == null)
        {
            dbPath = embeddingConfig.getDatabasePath();
        }

        EmbeddingModel model = EmbeddingModel.fromKey(modelKey);

        LOGGER.info("Query: {}", query);
        LOGGER.info("Model: {}", model.getKey());
        LOGGER.info("Database path: {}", dbPath);
        LOGGER.info("Limit: {}", limit);
        LOGGER.info("Threshold: {}", threshold != null ? threshold : model.getDefaultThreshold());

        try (SqliteVecConnection sqliteConnection = new SqliteVecConnection(dbPath);
             EmbeddingEngine engine = EmbeddingEngineFactory.create(model, embeddingConfig))
        {
            EmbeddingRepository repository = new EmbeddingRepository(sqliteConnection);
            SearchEngine searchEngine = new SearchEngine(engine, repository);

            List<SearchResult> results = searchEngine.search(query, limit, threshold);

            this.writeResults(results, objectMapper);
        }

        this.containerLifeCycle.stop();

        LOGGER.info("Completing {}.", this.getClass().getSimpleName());
    }

    private void writeResults(List<SearchResult> results, ObjectMapper objectMapper) throws IOException
    {
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);
        System.out.println(json);
    }
}
