package com.workflowy;

import java.util.*;

import com.gs.fw.finder.Operation;

public class NodeCollapsedList extends NodeCollapsedListAbstract {

	public NodeCollapsedList() {
		super();
	}

	public NodeCollapsedList(int initialSize) {
		super(initialSize);
	}

	public NodeCollapsedList(Collection c) {
		super(c);
	}

	public NodeCollapsedList(Operation operation) {
		super(operation);
	}
}
