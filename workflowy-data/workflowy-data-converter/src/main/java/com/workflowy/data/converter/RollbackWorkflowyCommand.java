package com.workflowy.data.converter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.annotation.Nonnull;

import cool.klass.data.store.DataStore;
import cool.klass.dropwizard.configuration.AbstractKlassConfiguration;
import io.dropwizard.Application;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.setup.Environment;
import io.liftwizard.reladomo.rollback.ReladomoTemporalRollback;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.thread.ShutdownThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rolls back the database to a state before a specified number of backup file imports.
 *
 * <p>This command is useful when you need to re-import backup files, for example:
 * <ul>
 *   <li>To recover from corrupted temporal state</li>
 *   <li>To re-process backup files with updated import logic</li>
 *   <li>To test import behavior on specific backup files</li>
 * </ul>
 *
 * <p>Example: {@code rollback-backups --backups-path /path/to/backups --count 3}
 * will find the 3rd-to-last backup file and roll back to just before its timestamp,
 * so that file and all subsequent files will be re-imported on the next import run.
 */
public class RollbackWorkflowyCommand<T extends AbstractKlassConfiguration> extends EnvironmentCommand<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(RollbackWorkflowyCommand.class);

	private final ContainerLifeCycle containerLifeCycle = new ContainerLifeCycle();

	public RollbackWorkflowyCommand(Application<T> application) {
		this(application, "rollback-backups", "Roll back the database to before a specified number of backup imports.");
	}

	protected RollbackWorkflowyCommand(Application<T> application, String name, String description) {
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
			.addArgument("--count")
			.type(Integer.class)
			.required(true)
			.help(
				"Number of backup files to roll back. The database will be rolled back to just before "
				+ "the Nth-to-last backup file's timestamp."
			);
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
		int count = namespace.getInt("count");

		LOGGER.info("backupsPath = {}", backupsPath);
		LOGGER.info("count = {}", count);

		Instant rollbackTarget = this.calculateRollbackTarget(backupsPath, count);

		if (rollbackTarget == null) {
			LOGGER.error(
				"Unable to calculate rollback target. Check that the backups directory contains enough files."
			);
			this.containerLifeCycle.stop();
			return;
		}

		LOGGER.info("Rolling back to: {}", rollbackTarget);

		var rollback = new ReladomoTemporalRollback(rollbackTarget);
		rollback.rollbackAllTables();

		WorkflowyDataConverter.resetWatermark(dataStore);

		this.containerLifeCycle.stop();

		LOGGER.info(
			"Completed {}. Run import-workflowy to re-import from backup files.",
			this.getClass().getSimpleName()
		);
	}

	/**
	 * Calculates the rollback target timestamp.
	 *
	 * @param backupsPath path to the directory containing backup files
	 * @param count number of backups to roll back
	 * @return the timestamp to roll back to, or null if calculation fails
	 */
	private Instant calculateRollbackTarget(Path backupsPath, int count) {
		ImmutableList<File> backupFiles = this.getBackupFiles(backupsPath);

		if (backupFiles.isEmpty()) {
			LOGGER.error("No backup files found in: {}", backupsPath);
			return null;
		}

		LOGGER.info("Found {} backup files", backupFiles.size());

		if (count < 1) {
			LOGGER.error("Count must be at least 1, got: {}", count);
			return null;
		}

		if (count > backupFiles.size()) {
			LOGGER.error(
				"Cannot roll back {} backups when only {} files exist. Rolling back all.",
				count,
				backupFiles.size()
			);
			count = backupFiles.size();
		}

		int targetIndex = backupFiles.size() - count;
		File targetFile = backupFiles.get(targetIndex);

		LOGGER.info("Target backup file (index {}): {}", targetIndex, targetFile.getName());

		Instant targetTimestamp = WorkflowyFileUtils.getFileTimestamp(targetFile);
		LOGGER.info("Target file timestamp: {}", targetTimestamp);

		Instant rollbackTarget = targetTimestamp.minus(1, ChronoUnit.MILLIS);
		LOGGER.info("Rollback target (1ms before): {}", rollbackTarget);

		return rollbackTarget;
	}

	private ImmutableList<File> getBackupFiles(Path backupsPath) {
		File directory = backupsPath.toFile();
		if (!directory.exists()) {
			throw new IllegalArgumentException("Backup directory does not exist: " + backupsPath);
		}
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("Backup path is not a directory: " + backupsPath);
		}
		File[] files = directory.listFiles((pathname) -> pathname.getName().endsWith(".workflowy.backup"));
		if (files == null) {
			throw new IllegalArgumentException("Unable to list files in: " + backupsPath);
		}
		return ArrayAdapter.adapt(files).toSortedListBy(File::getName).toImmutable();
	}
}
