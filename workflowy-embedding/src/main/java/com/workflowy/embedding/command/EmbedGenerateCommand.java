package com.workflowy.embedding.command;

import javax.annotation.Nonnull;

import com.workflowy.embedding.config.EmbeddingConfiguration;
import com.workflowy.embedding.config.EmbeddingConfigurationProvider;
import com.workflowy.embedding.engine.EmbeddingEngine;
import com.workflowy.embedding.engine.EmbeddingEngineFactory;
import com.workflowy.embedding.generator.EmbeddingGenerator;
import com.workflowy.embedding.generator.EmbeddingGenerator.GenerationResult;
import com.workflowy.embedding.model.EmbeddingModel;
import com.workflowy.embedding.repository.EmbeddingRepository;
import com.workflowy.embedding.repository.SqliteVecConnection;
import cool.klass.dropwizard.configuration.AbstractKlassConfiguration;
import io.dropwizard.Application;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.thread.ShutdownThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbedGenerateCommand<T extends AbstractKlassConfiguration & EmbeddingConfigurationProvider>
	extends EnvironmentCommand<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmbedGenerateCommand.class);

	private final ContainerLifeCycle containerLifeCycle = new ContainerLifeCycle();

	public EmbedGenerateCommand(Application<T> application) {
		super(application, "embed-generate", "Generate embeddings for all nodes");
	}

	@Override
	public void configure(Subparser subparser) {
		super.configure(subparser);

		subparser
			.addArgument("--model")
			.type(String.class)
			.setDefault("minilm")
			.help("Embedding model to use: minilm, mpnet, bge, openai-small, openai-large");

		subparser
			.addArgument("--batch-size")
			.type(Integer.class)
			.setDefault(100)
			.help("Number of nodes to process in each batch");

		subparser
			.addArgument("--force")
			.action(Arguments.storeTrue())
			.setDefault(false)
			.help("Force regeneration of all embeddings");

		subparser.addArgument("--db-path").type(String.class).help("Override path to the embeddings SQLite database");
	}

	@Override
	protected void run(@Nonnull Environment environment, Namespace namespace, @Nonnull T configuration)
		throws Exception {
		LOGGER.info("Running {}.", this.getClass().getSimpleName());

		environment.lifecycle().getManagedObjects().forEach(this.containerLifeCycle::addBean);
		ShutdownThread.register(this.containerLifeCycle);
		this.containerLifeCycle.start();

		EmbeddingConfiguration embeddingConfig = configuration.getEmbeddingConfiguration();

		String modelKey = namespace.getString("model");
		int batchSize = namespace.getInt("batch_size");
		boolean force = namespace.getBoolean("force");
		String dbPath = namespace.getString("db_path");

		if (dbPath == null) {
			dbPath = embeddingConfig.getDatabasePath();
		}

		EmbeddingModel model = EmbeddingModel.fromKey(modelKey);

		LOGGER.info("Model: {}", model.getKey());
		LOGGER.info("Database path: {}", dbPath);
		LOGGER.info("Batch size: {}", batchSize);
		LOGGER.info("Force: {}", force);

		try (
			SqliteVecConnection sqliteConnection = new SqliteVecConnection(dbPath);
			EmbeddingEngine engine = EmbeddingEngineFactory.create(model, embeddingConfig)
		) {
			EmbeddingRepository repository = new EmbeddingRepository(sqliteConnection);
			EmbeddingGenerator generator = new EmbeddingGenerator(engine, repository, batchSize, force);

			GenerationResult result = generator.generate((progress) ->
				LOGGER.info(
					"Progress: {}% ({}/{}) - Processed: {}, Skipped: {}, Errors: {}",
					progress.percentage(),
					progress.current(),
					progress.total(),
					progress.processed(),
					progress.skipped(),
					progress.errors()
				)
			);

			LOGGER.info("Generation complete!");
			LOGGER.info("Total nodes: {}", result.totalNodes());
			LOGGER.info("Processed: {}", result.processedCount());
			LOGGER.info("Skipped: {}", result.skippedCount());
			LOGGER.info("Errors: {}", result.errorCount());
		}

		this.containerLifeCycle.stop();

		LOGGER.info("Completing {}.", this.getClass().getSimpleName());
	}
}
