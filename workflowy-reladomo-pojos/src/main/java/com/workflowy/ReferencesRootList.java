package com.workflowy;

import java.util.*;

import com.gs.fw.finder.Operation;

public class ReferencesRootList extends ReferencesRootListAbstract {

	public ReferencesRootList() {
		super();
	}

	public ReferencesRootList(int initialSize) {
		super(initialSize);
	}

	public ReferencesRootList(Collection c) {
		super(c);
	}

	public ReferencesRootList(Operation operation) {
		super(operation);
	}
}
