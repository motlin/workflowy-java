package com.workflowy;

import java.sql.Timestamp;

import cool.klass.data.store.reladomo.UtcInfinityTimestamp;

public class Backlink extends BacklinkAbstract {

	public Backlink(Timestamp system) {
		super(system);
		// You must not modify this constructor. Mithra calls this internally.
		// You can call this constructor. You can also add new constructors.
	}

	public Backlink() {
		this(UtcInfinityTimestamp.getDefaultInfinity());
	}
}
