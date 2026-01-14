package com.workflowy;

import java.sql.Timestamp;
public class NodeS3File extends NodeS3FileAbstract
{
	public NodeS3File(Timestamp system
	)
	{
		super(system
		);
		// You must not modify this constructor. Mithra calls this internally.
		// You can call this constructor. You can also add new constructors.
	}

	public NodeS3File()
	{
		this(com.gs.fw.common.mithra.util.DefaultInfinityTimestamp.getDefaultInfinity());
	}
}
