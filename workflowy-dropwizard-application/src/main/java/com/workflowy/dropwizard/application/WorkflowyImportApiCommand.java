package com.workflowy.dropwizard.application;

import com.workflowy.data.converter.ImportWorkflowyApiCommand;

public class WorkflowyImportApiCommand extends ImportWorkflowyApiCommand<WorkflowyConfiguration> {

	public WorkflowyImportApiCommand(WorkflowyApplication application) {
		super(application);
	}
}
