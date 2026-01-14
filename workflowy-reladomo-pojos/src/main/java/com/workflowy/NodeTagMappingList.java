package com.workflowy;

import java.util.*;

import com.gs.fw.finder.Operation;

public class NodeTagMappingList extends NodeTagMappingListAbstract {

	public NodeTagMappingList() {
		super();
	}

	public NodeTagMappingList(int initialSize) {
		super(initialSize);
	}

	public NodeTagMappingList(Collection c) {
		super(c);
	}

	public NodeTagMappingList(Operation operation) {
		super(operation);
	}
}
