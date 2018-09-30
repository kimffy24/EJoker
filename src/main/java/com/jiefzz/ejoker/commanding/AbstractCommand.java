package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.infrastructure.MessageAbstract;
import com.jiefzz.ejoker.z.common.utils.Ensure;

public class AbstractCommand extends MessageAbstract implements ICommand {
	
	private String aggregateRootId;

	public AbstractCommand(){
		super();
	}
	
	public AbstractCommand(String aggregateRootId){
		Ensure.notNull(aggregateRootId, "aggregateRootId");
		this.aggregateRootId = aggregateRootId;
	}
	
	@Override
	public String getRoutingKey() {
		return this.aggregateRootId;
	}
	
	@Override
	public String getAggregateRootId() {
		return this.aggregateRootId;
	}

	@Override
	public void setAggregateRootId(String aggregateRootId) {
		Ensure.notNull(aggregateRootId, "aggregateRootId");
		this.aggregateRootId = aggregateRootId;
	}

}
