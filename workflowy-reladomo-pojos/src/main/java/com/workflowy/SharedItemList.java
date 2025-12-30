package com.workflowy;
import com.gs.fw.finder.Operation;
import java.util.*;
public class SharedItemList extends SharedItemListAbstract
{
	public SharedItemList()
	{
		super();
	}

	public SharedItemList(int initialSize)
	{
		super(initialSize);
	}

	public SharedItemList(Collection c)
	{
		super(c);
	}

	public SharedItemList(Operation operation)
	{
		super(operation);
	}
}
