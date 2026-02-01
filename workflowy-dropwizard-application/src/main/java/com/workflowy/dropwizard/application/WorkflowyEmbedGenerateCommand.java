package com.workflowy.dropwizard.application;

import com.workflowy.embedding.command.EmbedGenerateCommand;

public class WorkflowyEmbedGenerateCommand extends EmbedGenerateCommand<WorkflowyConfiguration> {

	public WorkflowyEmbedGenerateCommand(WorkflowyApplication application) {
		super(application);
	}
}
