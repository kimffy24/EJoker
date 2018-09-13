package com.jiefzz.ejoker.z.common.utils.relationship.taskTree;

public abstract class AssemblyTaskAbstract {
	
	AssemblyTaskAbstract prev = null;
	AssemblyTaskAbstract next = null;
	
	public abstract String process();
	
}
