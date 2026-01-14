package com.workflowy.dropwizard.application;

import javax.annotation.Nonnull;
import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smoketurner.dropwizard.graphql.GraphQLFactory;
import com.workflowy.embedding.config.EmbeddingConfiguration;
import com.workflowy.embedding.config.EmbeddingConfigurationProvider;
import cool.klass.dropwizard.configuration.AbstractKlassConfiguration;
import io.liftwizard.dropwizard.configuration.graphql.GraphQLFactoryProvider;
import io.liftwizard.servlet.config.singlepage.SinglePageRedirectFilterFactory;
import io.liftwizard.servlet.config.singlepage.SinglePageRedirectFilterFactoryProvider;

public class WorkflowyConfiguration
	extends AbstractKlassConfiguration
	implements GraphQLFactoryProvider, SinglePageRedirectFilterFactoryProvider, EmbeddingConfigurationProvider {

	@Nonnull
	private @Valid GraphQLFactory graphQL = new GraphQLFactory();

	private SinglePageRedirectFilterFactory singlePageRedirectFilterFactory = new SinglePageRedirectFilterFactory();

	@Nonnull
	private @Valid EmbeddingConfiguration embedding = new EmbeddingConfiguration();

	@Override
	@Nonnull
	@JsonProperty("graphQL")
	public GraphQLFactory getGraphQLFactory() {
		return this.graphQL;
	}

	@JsonProperty("graphQL")
	public void setGraphQLFactory(@Nonnull GraphQLFactory factory) {
		this.graphQL = factory;
	}

	@Override
	@JsonProperty("singlePageRedirectFilter")
	public SinglePageRedirectFilterFactory getSinglePageRedirectFilterFactory() {
		return this.singlePageRedirectFilterFactory;
	}

	@JsonProperty("singlePageRedirectFilter")
	public void setSinglePageRedirectFilterFactory(SinglePageRedirectFilterFactory singlePageRedirectFilterFactory) {
		this.singlePageRedirectFilterFactory = singlePageRedirectFilterFactory;
	}

	@Override
	@Nonnull
	@JsonProperty("embedding")
	public EmbeddingConfiguration getEmbeddingConfiguration() {
		return this.embedding;
	}

	@JsonProperty("embedding")
	public void setEmbeddingConfiguration(@Nonnull EmbeddingConfiguration embedding) {
		this.embedding = embedding;
	}
}
