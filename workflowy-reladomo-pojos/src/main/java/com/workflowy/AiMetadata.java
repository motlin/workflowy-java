package com.workflowy;

import java.sql.Timestamp;

import cool.klass.data.store.reladomo.UtcInfinityTimestamp;

public class AiMetadata extends AiMetadataAbstract {

	public AiMetadata(Timestamp system) {
		super(system);
		// You must not modify this constructor. Mithra calls this internally.
		// You can call this constructor. You can also add new constructors.
	}

	public AiMetadata() {
		this(UtcInfinityTimestamp.getDefaultInfinity());
	}
}
