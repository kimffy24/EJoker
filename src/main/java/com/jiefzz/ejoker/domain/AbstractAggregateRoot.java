package com.jiefzz.ejoker.domain;

import java.util.LinkedHashMap;

import com.jiefzz.ejoker.annotation.persistent.PersistentIgnore;
import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.infrastructure.ArgumentNullException;

public abstract class AbstractAggregateRoot<TAggregateRootId> implements IAggregateRoot<TAggregateRootId> {

	@PersistentIgnore
	private static final long serialVersionUID = -1803833835739801207L;
	
	private long _version=0;
	protected TAggregateRootId _id=null;

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
	public void setId(TAggregateRootId _id) {
		this._id = _id;
	}

	@Override
	public long getVersion() {
		return _version;
	}

	protected void ApplyEvent(IDomainEvent<TAggregateRootId> domainEvent) {

		if ( _id == null )
			throw new IllegalAggregateRootIdException("Domain Aggregate Id is null!!!");
		domainEvent.setAggregateRootId( _id );
		domainEvent.setVersion(_version+1);
		// 接收事件
		HandleEvent(domainEvent);
		// 提交事件到队列
		AppendUncommittedEvent(domainEvent);
	}
	protected void ApplyEvents(IDomainEvent<TAggregateRootId>[] domainEvents) {
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

	@Override
	public String getUniqueId() {
		return getId().toString();
	}

	private void verifyEvent(){}
}
