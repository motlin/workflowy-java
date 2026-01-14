package com.workflowy.data.converter;

import java.nio.file.Path;
import java.nio.file.Paths;

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

public class ImportWorkflowyCommand<T extends AbstractKlassConfiguration> extends EnvironmentCommand<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImportWorkflowyCommand.class);

	private final ContainerLifeCycle containerLifeCycle = new ContainerLifeCycle();

	public ImportWorkflowyCommand(Application<T> application) {
		this(application, "import-workflowy", "Import Workflowy backup files into the database.");
	}

	protected ImportWorkflowyCommand(Application<T> application, String name, String description) {
		super(application, name, description);
	}

	@Override
	public void configure(Subparser subparser) {
		super.configure(subparser);

		subparser
			.addArgument("--backups-path")
			.type(String.class)
			.required(true)
			.help("Path to the directory containing Workflowy backup files.");

		subparser
			.addArgument("--days-limit")
			.type(Integer.class)
			.setDefault(Integer.MAX_VALUE)
			.help("Maximum number of backup files to process (default: all).");
	}

	@Override
	protected void run(@Nonnull Environment environment, Namespace namespace, @Nonnull T configuration)
		throws Exception {
		LOGGER.info("Running {}.", this.getClass().getSimpleName());

		environment.lifecycle().getManagedObjects().forEach(this.containerLifeCycle::addBean);
		ShutdownThread.register(this.containerLifeCycle);
		this.containerLifeCycle.start();

		DataStore dataStore = configuration.getKlassFactory().getDataStoreFactory().createDataStore();

		String backupsPathString = namespace.getString("backups_path");
		Path backupsPath = Paths.get(backupsPathString);
		Integer daysLimit = namespace.getInt("days_limit");

		LOGGER.info("backupsPath = {}", backupsPath);
		LOGGER.info("daysLimit = {}", daysLimit);

		WorkflowyDataConverter.convert(backupsPath, environment.getObjectMapper(), dataStore, daysLimit);

		this.containerLifeCycle.stop();

		LOGGER.info("Completing {}.", this.getClass().getSimpleName());
	}
}
