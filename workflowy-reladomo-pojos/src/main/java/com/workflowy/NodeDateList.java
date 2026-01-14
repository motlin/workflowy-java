package com.workflowy;

import java.util.*;

import com.gs.fw.finder.Operation;

public class NodeDateList extends NodeDateListAbstract {

	public NodeDateList() {
		super();
	}

	public NodeDateList(int initialSize) {
		super(initialSize);
	}

	public NodeDateList(Collection c) {
		super(c);
	}

	public NodeDateList(Operation operation) {
		super(operation);
	}
}
