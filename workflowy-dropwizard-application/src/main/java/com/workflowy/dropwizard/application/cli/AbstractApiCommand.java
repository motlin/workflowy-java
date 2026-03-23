package com.workflowy.dropwizard.application.cli;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.workflowy.NodeContent;
import com.workflowy.NodeContentFinder;
import com.workflowy.NodeContentList;
import com.workflowy.data.converter.WorkflowyApiClient;
import com.workflowy.dropwizard.application.WorkflowyApplication;
import com.workflowy.dropwizard.application.WorkflowyConfiguration;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.thread.ShutdownThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractApiCommand extends EnvironmentCommand<WorkflowyConfiguration> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractApiCommand.class);

	private final ContainerLifeCycle containerLifeCycle = new ContainerLifeCycle();

	protected AbstractApiCommand(WorkflowyApplication application, String name, String description) {
		super(application, name, description);
	}

	@Override
	public void configure(Subparser subparser) {
		super.configure(subparser);
	}

	@Override
	protected void run(
		@Nonnull Environment environment,
		Namespace namespace,
		@Nonnull WorkflowyConfiguration configuration
	) throws Exception {
		LOGGER.info("Running {}.", this.getClass().getSimpleName());

		environment.lifecycle().getManagedObjects().forEach(this.containerLifeCycle::addBean);
		ShutdownThread.register(this.containerLifeCycle);
		this.containerLifeCycle.start();

		String apiKey = this.getApiKey();

		try (var apiClient = new WorkflowyApiClient(apiKey)) {
			this.executeCommand(namespace, configuration, apiClient);
		} finally {
			this.containerLifeCycle.stop();
		}

		LOGGER.info("Completing {}.", this.getClass().getSimpleName());
	}

	protected abstract void executeCommand(
		Namespace namespace,
		WorkflowyConfiguration configuration,
		WorkflowyApiClient apiClient
	) throws Exception;

	private String getApiKey() {
		String apiKey = System.getenv("WORKFLOWY_API_KEY");
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("WORKFLOWY_API_KEY environment variable is required");
		}
		return apiKey;
	}

	@Nullable
	protected String resolveNodeId(@Nullable String id, @Nullable String path, WorkflowyApiClient apiClient) {
		if (id != null) {
			return this.resolveShortOrFullId(id);
		}

		if (path != null) {
			return this.resolvePathToId(path, apiClient);
		}

		return null;
	}

	private String resolveShortOrFullId(String shortOrFullId) {
		if (shortOrFullId.length() == 36) {
			return shortOrFullId;
		}

		NodeContentList matches = NodeContentFinder.findMany(NodeContentFinder.id().startsWith(shortOrFullId));

		if (matches.isEmpty()) {
			throw new IllegalArgumentException("No node found with ID prefix: " + shortOrFullId);
		}
		if (matches.size() > 1) {
			throw new IllegalArgumentException("Multiple nodes match prefix: " + shortOrFullId);
		}

		return matches.get(0).getId();
	}

	@Nullable
	private String resolvePathToId(String commaSeparatedPath, WorkflowyApiClient apiClient) {
		List<String> segments = Arrays.stream(commaSeparatedPath.split(",")).map(String::trim).toList();

		// Try H2 cache first
		String currentId = null;
		for (String segment : segments) {
			NodeContentList children;
			if (currentId == null) {
				children = NodeContentFinder.findMany(NodeContentFinder.parentId().isNull());
			} else {
				children = NodeContentFinder.findMany(NodeContentFinder.parentId().eq(currentId));
			}

			NodeContent match = null;
			for (int i = 0; i < children.size(); i++) {
				NodeContent child = children.get(i);
				if (segment.equals(child.getName())) {
					match = child;
					break;
				}
			}

			if (match == null) {
				// Fall back to API
				var apiNode = apiClient.findNodeByPath(segments);
				return apiNode != null ? apiNode.id() : null;
			}

			currentId = match.getId();
		}

		return currentId;
	}

	protected static String getWorkflowyUrl(String nodeId) {
		String hex = nodeId.replace("-", "");
		String shortId = hex.substring(hex.length() - 12);
		return "https://workflowy.com/#/" + shortId;
	}
}
