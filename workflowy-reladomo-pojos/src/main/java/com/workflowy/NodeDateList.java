package com.workflowy;
import com.gs.fw.finder.Operation;
import java.util.*;
public class NodeDateList extends NodeDateListAbstract
{
	public NodeDateList()
	{
		super();
	}

	public NodeDateList(int initialSize)
	{
		super(initialSize);
	}

	public NodeDateList(Collection c)
	{
		super(c);
	}

	public NodeDateList(Operation operation)
	{
		super(operation);
	}
}
