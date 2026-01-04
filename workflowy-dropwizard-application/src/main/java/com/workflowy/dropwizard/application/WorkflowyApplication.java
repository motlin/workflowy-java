package com.workflowy.dropwizard.application;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import cool.klass.dropwizard.bundle.graphql.KlassGraphQLBundle;
import cool.klass.serialization.jackson.module.meta.model.module.KlassMetaModelJacksonModule;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.liftwizard.dropwizard.bundle.httplogging.JerseyHttpLoggingBundle;
import io.liftwizard.servlet.logging.logstash.encoder.StructuredArgumentsLogstashEncoderLogger;
import io.liftwizard.servlet.logging.mdc.StructuredArgumentsMDCLogger;
import io.liftwizard.servlet.logging.typesafe.StructuredArguments;

public class WorkflowyApplication
        extends AbstractWorkflowyApplication
{
    public static void main(String[] args)
            throws Exception
    {
        new WorkflowyApplication().run(args);
    }

    @Override
    public void initialize(@Nonnull Bootstrap<WorkflowyConfiguration> bootstrap)
    {
        super.initialize(bootstrap);
    }

    @Override
    protected void initializeCommands(@Nonnull Bootstrap<WorkflowyConfiguration> bootstrap)
    {
        super.initializeCommands(bootstrap);
        bootstrap.addCommand(new WorkflowyImportCommand(this));
    }

    @Override
    protected void initializeBundles(@Nonnull Bootstrap<WorkflowyConfiguration> bootstrap)
    {
        super.initializeBundles(bootstrap);

        var mdcLogger = new StructuredArgumentsMDCLogger(bootstrap.getObjectMapper());
        var logstashLogger = new StructuredArgumentsLogstashEncoderLogger();

        Consumer<StructuredArguments> structuredLogger = structuredArguments ->
        {
            mdcLogger.accept(structuredArguments);
            logstashLogger.accept(structuredArguments);
        };

        bootstrap.addBundle(new JerseyHttpLoggingBundle(structuredLogger));

        bootstrap.addBundle(new KlassGraphQLBundle<>());

        bootstrap.addBundle(new MigrationsBundle<>()
        {
            @Override
            public DataSourceFactory getDataSourceFactory(WorkflowyConfiguration configuration)
            {
                return configuration.getNamedDataSourcesFactory().getNamedDataSourceFactoryByName("h2-tcp");
            }
        });
    }

    @Override
    protected void registerJacksonModules(@Nonnull Environment environment)
    {
        super.registerJacksonModules(environment);

        environment.getObjectMapper().registerModule(new KlassMetaModelJacksonModule());
    }
}
