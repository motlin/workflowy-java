package com.workflowy;

import java.sql.Timestamp;

import cool.klass.data.store.reladomo.UtcInfinityTimestamp;

public class NodeCalendarLevels extends NodeCalendarLevelsAbstract {

	public NodeCalendarLevels(Timestamp system) {
		super(system);
		// You must not modify this constructor. Mithra calls this internally.
		// You can call this constructor. You can also add new constructors.
	}

	public NodeCalendarLevels() {
		this(UtcInfinityTimestamp.getDefaultInfinity());
	}
}
