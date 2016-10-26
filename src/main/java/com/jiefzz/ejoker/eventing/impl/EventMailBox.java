package com.jiefzz.ejoker.eventing.impl;

import java.util.Collection;
import java.util.concurrent.Future;

import com.jiefzz.ejoker.eventing.EventCommittingConetxt;
import com.jiefzz.ejoker.z.common.system.delegate.Action;

public class EventMailBox {

	private final String aggregateRootId;
	
	public final Action<Collection<EventCommittingConetxt>> handleMessageAction;
	private int isHandlingMessage;
	private int batchSize;
	
	public EventMailBox(String aggregateRootId, int batchSize, Action<Collection<EventCommittingConetxt>> handleMessageAction) {
		this.aggregateRootId = aggregateRootId;
		this.batchSize = batchSize;
		this.handleMessageAction = handleMessageAction;
	}
	
	public String getAggregateRootId() {
		return this.aggregateRootId;
	}
	
}
