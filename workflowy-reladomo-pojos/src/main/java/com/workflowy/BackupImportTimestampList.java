package com.workflowy;

import java.util.*;

import com.gs.fw.finder.Operation;

public class BackupImportTimestampList extends BackupImportTimestampListAbstract {

	public BackupImportTimestampList() {
		super();
	}

	public BackupImportTimestampList(int initialSize) {
		super(initialSize);
	}

	public BackupImportTimestampList(Collection c) {
		super(c);
	}

	public BackupImportTimestampList(Operation operation) {
		super(operation);
	}
}
