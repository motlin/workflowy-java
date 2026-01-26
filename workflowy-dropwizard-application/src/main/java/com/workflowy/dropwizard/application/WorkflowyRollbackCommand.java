package com.workflowy.dropwizard.application;

import com.workflowy.data.converter.RollbackWorkflowyCommand;

public class WorkflowyRollbackCommand extends RollbackWorkflowyCommand<WorkflowyConfiguration> {

	public WorkflowyRollbackCommand(WorkflowyApplication application) {
		super(application);
	}
}
