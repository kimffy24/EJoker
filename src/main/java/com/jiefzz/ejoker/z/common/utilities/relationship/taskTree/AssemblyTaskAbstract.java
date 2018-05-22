package com.jiefzz.ejoker.z.common.utilities.relationship.taskTree;

public abstract class AssemblyTaskAbstract {
	
	AssemblyTaskAbstract prev = null;
	AssemblyTaskAbstract next = null;
	
	public abstract String process();
	
}
