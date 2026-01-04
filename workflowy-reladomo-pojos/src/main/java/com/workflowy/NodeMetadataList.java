package com.workflowy;
import com.gs.fw.finder.Operation;
import java.util.*;
public class NodeMetadataList extends NodeMetadataListAbstract
{
	public NodeMetadataList()
	{
		super();
	}

	public NodeMetadataList(int initialSize)
	{
		super(initialSize);
	}

	public NodeMetadataList(Collection c)
	{
		super(c);
	}

	public NodeMetadataList(Operation operation)
	{
		super(operation);
	}
}
