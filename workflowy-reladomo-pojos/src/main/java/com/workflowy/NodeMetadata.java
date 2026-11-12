package com.workflowy;

import java.sql.Timestamp;

import cool.klass.reladomo.utc.infinity.timestamp.UtcInfinityTimestamp;

public class NodeMetadata
	extends NodeMetadataAbstract
{
	public NodeMetadata(Timestamp system)
	{
		super(system);
		// You must not modify this constructor. Mithra calls this internally.
		// You can call this constructor. You can also add new constructors.
	}

	public NodeMetadata()
	{
		this(UtcInfinityTimestamp.getDefaultInfinity());
	}

	public boolean isCompleted()
	{
		return this.getCompletedAt() != null;
	}
}
