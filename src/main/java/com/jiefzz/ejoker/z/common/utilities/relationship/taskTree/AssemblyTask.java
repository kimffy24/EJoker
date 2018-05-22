package com.jiefzz.ejoker.z.common.utilities.relationship.taskTree;

public class AssemblyTask<ContainerKVP, ContainerVP> {
	
	private volatile AssemblyTaskAbstract start = null;
	private volatile AssemblyTaskAbstract end = null;
	
	public AssemblyTask() {}
	
	public AssemblyTask<ContainerKVP, ContainerVP> enqueue(AssemblyTaskAbstract task) {
		internalAdd(task);
		return this;
	}
	
	public AssemblyTaskAbstract poll() {
		if(null == this.start)
			return null;
		AssemblyTaskAbstract willBePoll = this.start;
		this.start = willBePoll.next;
		this.start.prev = null;
		willBePoll.next = null;
		if(willBePoll.equals(end)) {
			end = null;
		}
		return willBePoll;
	}

	private void internalAdd(AssemblyTaskAbstract node) {
		if(end == null) {
			start = end = node;
		} else {
			end.next = node;
			node.prev = end;
			end = node;
		}
	}

}
