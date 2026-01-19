package com.workflowy;

import java.sql.Timestamp;

import cool.klass.data.store.reladomo.UtcInfinityTimestamp;

public class NodeCalendar extends NodeCalendarAbstract {

	public NodeCalendar(Timestamp system) {
		super(system);
		// You must not modify this constructor. Mithra calls this internally.
		// You can call this constructor. You can also add new constructors.
	}

	public NodeCalendar() {
		this(UtcInfinityTimestamp.getDefaultInfinity());
	}
}
