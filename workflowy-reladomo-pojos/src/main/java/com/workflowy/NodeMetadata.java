package com.workflowy;

import java.sql.Timestamp;

import com.gs.fw.common.mithra.util.DefaultInfinityTimestamp;

public class NodeMetadata extends NodeMetadataAbstract {

	public NodeMetadata(Timestamp system) {
		super(system);
		// You must not modify this constructor. Mithra calls this internally.
		// You can call this constructor. You can also add new constructors.
	}

	public NodeMetadata() {
		this(DefaultInfinityTimestamp.getDefaultInfinity());
	}
}
