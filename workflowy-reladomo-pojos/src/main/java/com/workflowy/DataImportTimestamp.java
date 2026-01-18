package com.workflowy;

import java.sql.Timestamp;

import cool.klass.data.store.reladomo.UtcInfinityTimestamp;

public class DataImportTimestamp extends DataImportTimestampAbstract {

	public DataImportTimestamp(Timestamp system) {
		super(system);
		// You must not modify this constructor. Mithra calls this internally.
		// You can call this constructor. You can also add new constructors.
	}

	public DataImportTimestamp() {
		this(UtcInfinityTimestamp.getDefaultInfinity());
	}
}
