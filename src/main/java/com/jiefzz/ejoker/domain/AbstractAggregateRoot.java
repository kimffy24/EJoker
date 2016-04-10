package com.jiefzz.ejoker.domain;

import java.util.LinkedHashMap;

import com.jiefzz.ejoker.annotation.persistent.PersistentIgnore;
import com.jiefzz.ejoker.eventing.IDomainEvent;

public abstract class AbstractAggregateRoot<TAggregateRootId> implements IAggregateRoot<TAggregateRootId> {

	/**
	 * 
	 */
	@PersistentIgnore
	private static final long serialVersionUID = -1803833835739801207L;
	
	long _version=0;
	TAggregateRootId _id=null;

	protected AbstractAggregateRoot(TAggregateRootId id) throws Exception {
		if (id == null)
			throw new Exception();
		_id = id;
	}

	protected AbstractAggregateRoot(TAggregateRootId id, long version) throws Exception {
		this(id);
		if (version < 0)
			throw new Exception(/*"Version cannot small than zero, aggregateRootId: {0}, version: {1}", id, version*/);
		_version = version;
	}
	
	@Override
	public TAggregateRootId getId(){
		return _id;
	}

	@Override
	public AbstractAggregateRoot<TAggregateRootId> setId(TAggregateRootId _id) {
		this._id = _id;
		return this;
	}

	@Override
	public long getVersion() {
		return _version;
	}

	protected void ApplyEvent(IDomainEvent<TAggregateRootId> domainEvent) throws Exception {

		if (_id == null)
			throw new Exception();
		domainEvent.setAggregateRootId(_id);
		domainEvent.setVersion(_version+1);
		HandleEvent(domainEvent);
		AppendUncommittedEvent(domainEvent);
		
	}
	protected void ApplyEvents(IDomainEvent<TAggregateRootId>[] domainEvents) throws Exception {
		for (IDomainEvent<TAggregateRootId> domainEvent : domainEvents)
			ApplyEvent(domainEvent);
	}
	
	private void HandleEvent(IDomainEvent<TAggregateRootId> domainEvent){}
	private void AppendUncommittedEvent(IDomainEvent<TAggregateRootId> domainEvent){}

	@Override
	public LinkedHashMap<Integer, String> GetChanges() { return null; }

	@Override
	public void AcceptChanges(int newVersion) {}

	@Override
	public void ReplayEvents(LinkedHashMap<Integer, String> eventStreams) {}

}
