package com.workflowy;
import java.sql.Timestamp;

import com.gs.fw.common.mithra.util.DefaultInfinityTimestamp;

public class VirtualRootMapping extends VirtualRootMappingAbstract
{
	public VirtualRootMapping(Timestamp system
	)
	{
		super(system
		);
		// You must not modify this constructor. Mithra calls this internally.
		// You can call this constructor. You can also add new constructors.
	}

	public VirtualRootMapping()
	{
		this(DefaultInfinityTimestamp.getDefaultInfinity());
	}
}
