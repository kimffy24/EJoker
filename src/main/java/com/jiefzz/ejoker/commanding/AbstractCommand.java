package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.annotation.persistent.PersistentIgnore;
import com.jiefzz.ejoker.infrastructure.AbstractMessage;
import com.jiefzz.ejoker.infrastructure.common.ArgumentNullException;

public class AbstractCommand extends AbstractMessage implements ICommand {
	
	@PersistentIgnore
	private static final long serialVersionUID = 8151751740813634355L;
	
	private String aggregateRootId;

	public AbstractCommand(){
		super();
	}
	
	public AbstractCommand(String aggregateRootId){
		if (aggregateRootId == null) throw new ArgumentNullException("aggregateRootId");
		this.aggregateRootId = aggregateRootId;
	}
	
	@Override
	public String GetRoutingKey() {
		return this.aggregateRootId;
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
