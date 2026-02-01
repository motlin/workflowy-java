package com.workflowy.dropwizard.application;

import com.workflowy.embedding.command.SearchCommand;

public class WorkflowySearchCommand extends SearchCommand<WorkflowyConfiguration> {

	public WorkflowySearchCommand(WorkflowyApplication application) {
		super(application);
	}
}
