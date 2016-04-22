package com.jiefzz.ejoker.domain;

import java.util.LinkedHashMap;

import com.jiefzz.ejoker.annotation.persistent.PersistentIgnore;
import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.infrastructure.ArgumentNullException;

public abstract class AbstractAggregateRoot<TAggregateRootId> implements IAggregateRoot<TAggregateRootId> {

	/**
	 * 
	 */
	@PersistentIgnore
	private static final long serialVersionUID = -1803833835739801207L;
	
	long _version=0;
	TAggregateRootId _id=null;

	protected AbstractAggregateRoot(TAggregateRootId id) {
		if (id == null)
			throw new ArgumentNullException("id");
		_id = id;
	}

	protected AbstractAggregateRoot(TAggregateRootId id, long version) {
		this(id);
		if (version < 0)
			throw new ArgumentNullException("version");
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

	protected void ApplyEvent(IDomainEvent domainEvent) throws Exception {

		if ( _id == null )
			throw new IllegalAggregateRootIdException("Domain Aggregate Id is null!!!");
		domainEvent.setAggregateRootId( _id.toString() );
		domainEvent.setVersion(_version+1);
		HandleEvent(domainEvent);
		AppendUncommittedEvent(domainEvent);
		
	}
	protected void ApplyEvents(IDomainEvent[] domainEvents) throws Exception {
		for (IDomainEvent domainEvent : domainEvents)
			ApplyEvent(domainEvent);
	}
	
	private void HandleEvent(IDomainEvent domainEvent){}
	private void AppendUncommittedEvent(IDomainEvent domainEvent){}

	@Override
	public LinkedHashMap<Integer, String> GetChanges() { return null; }

	@Override
	public void AcceptChanges(int newVersion) {}

	@Override
	public void ReplayEvents(LinkedHashMap<Integer, String> eventStreams) {}

}
