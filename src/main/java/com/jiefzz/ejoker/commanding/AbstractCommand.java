package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.infrastructure.AbstractMessage;
import com.jiefzz.ejoker.z.common.utils.Ensure;

public class AbstractCommand extends AbstractMessage implements ICommand {
	
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

	public void setAggregateRootId(String aggregateRootId) {
		Ensure.notNull(aggregateRootId, "aggregateRootId");
		this.aggregateRootId = aggregateRootId;
	}

}
