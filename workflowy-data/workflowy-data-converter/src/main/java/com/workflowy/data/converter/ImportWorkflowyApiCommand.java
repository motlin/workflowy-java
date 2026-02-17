package com.workflowy.data.converter;

import java.io.File;

import javax.annotation.Nonnull;

import cool.klass.data.store.DataStore;
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

/**
 * CLI command to import Workflowy data from an API export JSON file.
 *
 * <p>The API export format differs from the backup format:
 * <ul>
 *   <li>Flat structure with explicit parent_id (no nested children)</li>
 *   <li>Uses standard Unix timestamps</li>
 *   <li>Obtained from GET https://workflowy.com/api/v1/nodes-export</li>
 * </ul>
 *
 * <p>Usage: {@code java -jar app.jar import-workflowy-api --api-export-file /path/to/export.json config.yml}
 */
public class ImportWorkflowyApiCommand<T extends AbstractKlassConfiguration> extends EnvironmentCommand<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImportWorkflowyApiCommand.class);

	private final ContainerLifeCycle containerLifeCycle = new ContainerLifeCycle();

	public ImportWorkflowyApiCommand(Application<T> application) {
		this(application, "import-workflowy-api", "Import Workflowy API export file into the database.");
	}

	protected ImportWorkflowyApiCommand(Application<T> application, String name, String description) {
		super(application, name, description);
	}

	@Override
	public void configure(Subparser subparser) {
		super.configure(subparser);

		subparser
			.addArgument("--api-export-file")
			.type(String.class)
			.required(true)
			.help("Path to the API export JSON file (from /api/v1/nodes-export).");
	}

	@Override
	protected void run(@Nonnull Environment environment, Namespace namespace, @Nonnull T configuration)
		throws Exception {
		LOGGER.info("Running {}.", this.getClass().getSimpleName());

		environment.lifecycle().getManagedObjects().forEach(this.containerLifeCycle::addBean);
		ShutdownThread.register(this.containerLifeCycle);
		this.containerLifeCycle.start();

		DataStore dataStore = configuration.getKlassFactory().getDataStoreFactory().createDataStore();

		String apiExportFileString = namespace.getString("api_export_file");
		var apiExportFile = new File(apiExportFileString);

		if (!apiExportFile.exists()) {
			throw new IllegalArgumentException("API export file not found: " + apiExportFileString);
		}

		LOGGER.info("apiExportFile = {}", apiExportFile);

		WorkflowyApiDataConverter.convertFromFile(apiExportFile, environment.getObjectMapper(), dataStore);

		this.containerLifeCycle.stop();

		LOGGER.info("Completing {}.", this.getClass().getSimpleName());
	}
}
