package com.workflowy;
import com.gs.fw.finder.Operation;
import java.util.*;
public class NodeS3FileList extends NodeS3FileListAbstract
{
	public NodeS3FileList()
	{
		super();
	}

	public NodeS3FileList(int initialSize)
	{
		super(initialSize);
	}

	public NodeS3FileList(Collection c)
	{
		super(c);
	}

	public NodeS3FileList(Operation operation)
	{
		super(operation);
	}
}
