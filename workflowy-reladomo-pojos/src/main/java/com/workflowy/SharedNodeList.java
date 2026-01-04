package com.workflowy;
import com.gs.fw.finder.Operation;
import java.util.*;
public class SharedNodeList extends SharedNodeListAbstract
{
	public SharedNodeList()
	{
		super();
	}

	public SharedNodeList(int initialSize)
	{
		super(initialSize);
	}

	public SharedNodeList(Collection c)
	{
		super(c);
	}

	public SharedNodeList(Operation operation)
	{
		super(operation);
	}
}
