package com.workflowy;

import java.util.*;

import com.gs.fw.finder.Operation;

public class SharedNodeList extends SharedNodeListAbstract {

	public SharedNodeList() {
		super();
	}

	public SharedNodeList(int initialSize) {
		super(initialSize);
	}

	public SharedNodeList(Collection c) {
		super(c);
	}

	public SharedNodeList(Operation operation) {
		super(operation);
	}
}
