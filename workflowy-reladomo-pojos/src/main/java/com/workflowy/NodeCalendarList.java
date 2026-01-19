package com.workflowy;

import java.util.*;

import com.gs.fw.finder.Operation;

public class NodeCalendarList extends NodeCalendarListAbstract {

	public NodeCalendarList() {
		super();
	}

	public NodeCalendarList(int initialSize) {
		super(initialSize);
	}

	public NodeCalendarList(Collection c) {
		super(c);
	}

	public NodeCalendarList(Operation operation) {
		super(operation);
	}
}
