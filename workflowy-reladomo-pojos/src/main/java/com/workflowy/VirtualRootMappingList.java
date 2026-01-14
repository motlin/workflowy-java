package com.workflowy;

import java.util.*;

import com.gs.fw.finder.Operation;

public class VirtualRootMappingList extends VirtualRootMappingListAbstract {

	public VirtualRootMappingList() {
		super();
	}

	public VirtualRootMappingList(int initialSize) {
		super(initialSize);
	}

	public VirtualRootMappingList(Collection c) {
		super(c);
	}

	public VirtualRootMappingList(Operation operation) {
		super(operation);
	}
}
