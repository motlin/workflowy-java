package com.workflowy;
import com.gs.fw.finder.Operation;
import java.util.*;
public class VirtualRootMappingList extends VirtualRootMappingListAbstract
{
	public VirtualRootMappingList()
	{
		super();
	}

	public VirtualRootMappingList(int initialSize)
	{
		super(initialSize);
	}

	public VirtualRootMappingList(Collection c)
	{
		super(c);
	}

	public VirtualRootMappingList(Operation operation)
	{
		super(operation);
	}
}
