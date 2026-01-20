package com.workflowy;
import java.sql.Timestamp;
public class NodeCalendarLevels extends NodeCalendarLevelsAbstract
{
	public NodeCalendarLevels(Timestamp system
	)
	{
		super(system
		);
		// You must not modify this constructor. Mithra calls this internally.
		// You can call this constructor. You can also add new constructors.
	}

	public NodeCalendarLevels()
	{
		this(cool.klass.data.store.reladomo.UtcInfinityTimestamp.getDefaultInfinity());
	}
}
