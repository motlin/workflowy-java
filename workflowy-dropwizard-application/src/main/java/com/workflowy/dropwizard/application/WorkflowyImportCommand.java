package com.workflowy.dropwizard.application;

import com.workflowy.data.converter.ImportWorkflowyCommand;

public class WorkflowyImportCommand extends ImportWorkflowyCommand<WorkflowyConfiguration> {

	public WorkflowyImportCommand(WorkflowyApplication application) {
		super(application);
	}
}
