package com.workflowy;

import java.util.*;

import com.gs.fw.finder.Operation;

public class NodeMetadataList extends NodeMetadataListAbstract {

	public NodeMetadataList() {
		super();
	}

	public NodeMetadataList(int initialSize) {
		super(initialSize);
	}

	public NodeMetadataList(Collection c) {
		super(c);
	}

	public NodeMetadataList(Operation operation) {
		super(operation);
	}
}
