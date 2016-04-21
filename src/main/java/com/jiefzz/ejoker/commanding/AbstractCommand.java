package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.infrastructure.AbstractMessage;
import com.jiefzz.ejoker.infrastructure.ArgumentNullException;

public class AbstractCommand extends AbstractMessage implements ICommand {
	
	private static final long serialVersionUID = 8151751740813634355L;
	
	private String aggregateRootId;

	public AbstractCommand(){
		super();
	}
	
	@Override
	public String GetRoutingKey() {
		return this.aggregateRootId;
	}
	
	public AbstractCommand(String aggregateRootId){
		if (aggregateRootId == null) throw new ArgumentNullException("aggregateRootId");
		this.aggregateRootId = aggregateRootId;
	}
	
	
	@Override
	public String getAggregateRootId() {
		return this.aggregateRootId;
	}

	@Override
	public void setAggregateRootId(String aggregateRootId) {
		if (aggregateRootId == null) throw new ArgumentNullException("aggregateRootId");
		this.aggregateRootId = aggregateRootId;
	}

}
