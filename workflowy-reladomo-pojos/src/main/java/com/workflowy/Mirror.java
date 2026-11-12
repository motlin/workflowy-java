package com.workflowy;

import java.sql.Timestamp;

import cool.klass.reladomo.utc.infinity.timestamp.UtcInfinityTimestamp;

public class Mirror
	extends MirrorAbstract
{
	public Mirror(Timestamp system)
	{
		super(system);
		// You must not modify this constructor. Mithra calls this internally.
		// You can call this constructor. You can also add new constructors.
	}

	public Mirror()
	{
		this(UtcInfinityTimestamp.getDefaultInfinity());
	}
}
