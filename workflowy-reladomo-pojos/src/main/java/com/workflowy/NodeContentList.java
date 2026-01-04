package com.workflowy;
import com.gs.fw.finder.Operation;
import java.util.*;
public class NodeContentList extends NodeContentListAbstract
{
	public NodeContentList()
	{
		super();
	}

	public NodeContentList(int initialSize)
	{
		super(initialSize);
	}

	public NodeContentList(Collection c)
	{
		super(c);
	}

	public NodeContentList(Operation operation)
	{
		super(operation);
	}
}
