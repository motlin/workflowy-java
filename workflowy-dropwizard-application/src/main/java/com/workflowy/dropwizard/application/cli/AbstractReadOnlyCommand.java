package com.workflowy.dropwizard.application.cli;

import java.io.IOException;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Subparser;
import com.gs.fw.common.mithra.finder.Operation;
import com.workflowy.NodeContent;
import com.workflowy.NodeContentFinder;
import com.workflowy.NodeContentList;
import com.workflowy.dropwizard.application.WorkflowyConfiguration;
import io.dropwizard.Application;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.thread.ShutdownThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractReadOnlyCommand
        extends EnvironmentCommand<WorkflowyConfiguration>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractReadOnlyCommand.class);

    private final ContainerLifeCycle containerLifeCycle = new ContainerLifeCycle();
    protected ObjectMapper objectMapper;
    private boolean colorOutput;

    protected AbstractReadOnlyCommand(
            Application<WorkflowyConfiguration> application,
            String name,
            String description)
    {
        super(application, name, description);
    }

    @Override
    public void configure(Subparser subparser)
    {
        super.configure(subparser);
        subparser.addArgument("--color")
                .action(Arguments.storeTrue())
                .setDefault(false)
                .dest("color")
                .help("Enable colored JSON output");
        subparser.addArgument("--no-color")
                .action(Arguments.storeFalse())
                .dest("color")
                .help("Disable colored JSON output");
    }

    @Override
    protected void run(
            @Nonnull Environment environment,
            Namespace namespace,
            @Nonnull WorkflowyConfiguration configuration)
            throws Exception
    {
        LOGGER.info("Running {}.", this.getClass().getSimpleName());

        environment.lifecycle().getManagedObjects().forEach(this.containerLifeCycle::addBean);
        ShutdownThread.register(this.containerLifeCycle);
        this.containerLifeCycle.start();

        this.objectMapper = environment.getObjectMapper();
        this.colorOutput = namespace.getBoolean("color");

        try
        {
            Object result = this.executeCommand(namespace, configuration);
            this.writeJsonOutput(result);
        }
        catch (CommandException e)
        {
            this.writeErrorOutput(e);
        }
        finally
        {
            this.containerLifeCycle.stop();
        }

        LOGGER.info("Completing {}.", this.getClass().getSimpleName());
    }

    protected abstract Object executeCommand(Namespace namespace, WorkflowyConfiguration configuration)
            throws CommandException;

    protected void writeJsonOutput(Object result) throws IOException
    {
        String json = this.objectMapper.writer(new JsonPrettyPrinter())
                .writeValueAsString(result);

        if (this.colorOutput)
        {
            json = JsonSyntaxHighlighter.highlight(json);
        }

        System.out.print(json);
    }

    protected void writeErrorOutput(CommandException e) throws IOException
    {
        ErrorResponse error = new ErrorResponse(e.getCode(), e.getMessage());
        this.writeJsonOutput(error);
    }

    protected String resolveNodeId(String shortOrFullId) throws CommandException
    {
        if (shortOrFullId == null)
        {
            return null;
        }

        // Full UUID format: 36 chars (with hyphens)
        if (shortOrFullId.length() == 36)
        {
            return shortOrFullId;
        }

        // Short ID: Search by prefix
        Operation operation = NodeContentFinder.id().startsWith(shortOrFullId);
        NodeContentList matches = NodeContentFinder.findMany(operation);

        if (matches.isEmpty())
        {
            throw new CommandException("NOT_FOUND", "No node found with ID prefix: " + shortOrFullId);
        }
        if (matches.size() > 1)
        {
            throw new CommandException("AMBIGUOUS_ID", "Multiple nodes match prefix: " + shortOrFullId);
        }

        return matches.get(0).getId();
    }

    protected NodeContent findNodeById(String nodeId) throws CommandException
    {
        Operation operation = NodeContentFinder.id().eq(nodeId);
        NodeContent node = NodeContentFinder.findOne(operation);

        if (node == null)
        {
            throw new CommandException("NOT_FOUND", "Node not found: " + nodeId);
        }

        return node;
    }
}
