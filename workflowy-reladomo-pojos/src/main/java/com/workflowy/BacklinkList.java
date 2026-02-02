package com.workflowy;

import java.util.*;

import com.gs.fw.finder.Operation;

public class BacklinkList extends BacklinkListAbstract {

	public BacklinkList() {
		super();
	}

	public BacklinkList(int initialSize) {
		super(initialSize);
	}

	public BacklinkList(Collection c) {
		super(c);
	}

	public BacklinkList(Operation operation) {
		super(operation);
	}
}
