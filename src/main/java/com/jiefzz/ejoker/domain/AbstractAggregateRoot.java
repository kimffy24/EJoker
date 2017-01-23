package com.jiefzz.ejoker.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.jiefzz.ejoker.domain.helper.AggregateHandlerJavaHelper;
import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.z.common.ArgumentException;
import com.jiefzz.ejoker.z.common.ArgumentNullException;
import com.jiefzz.ejoker.z.common.InvalidOperationException;
import com.jiefzz.ejoker.z.common.context.annotation.persistent.PersistentIgnore;

public abstract class AbstractAggregateRoot<TAggregateRootId> implements IAggregateRoot {

	@PersistentIgnore
	private static final long serialVersionUID = -1803833835739801207L;

	private long version=0;
	
	private Lock lock4ApplyEvent = new ReentrantLock();

	@PersistentIgnore
	private List<IDomainEvent<?>> uncommittedEvents = new ArrayList<IDomainEvent<?>>();
	
	protected TAggregateRootId id=null;
	
	protected AbstractAggregateRoot(){
	}

	protected AbstractAggregateRoot(TAggregateRootId id) {
		this();
		if (id == null)
			throw new ArgumentNullException("id");
		this.id = id;
	}

	protected AbstractAggregateRoot(TAggregateRootId id, long version) {
		this(id);
		if (version < 0)
			throw new ArgumentException(String.format("Version can not small than 0, aggregateRootId=%s version=%s", id, version));
		this.version = version;
	}

	public TAggregateRootId getId(){
		return id;
	}

	public void setId(TAggregateRootId id) {
		this.id = id;
	}

	@Override
	public String getUniqueId() {
		return null==id?null:id.toString();
	}

	@Override
	public long getVersion() {
		return version;
	}
	
	@SuppressWarnings("unchecked")
	public <TRole> TRole actAs(Class<TRole> clazz) {
		if ( !clazz.isInterface() )
			throw new RuntimeException("This class could not act as ["+clazz.getName()+"]");
		if ( !clazz.isAssignableFrom(this.getClass()))
			throw new RuntimeException("This class could not act as ["+clazz.getName()+"]");
		return (TRole )this;
	}

	protected void applyEvent(IDomainEvent<TAggregateRootId> domainEvent) {
		try {
			lock4ApplyEvent.lock();
			internalApplyEvent(domainEvent);
		} finally {
			lock4ApplyEvent.unlock();
		}
	}
	protected void applyEvents(IDomainEvent<TAggregateRootId>[] domainEvents) {
		try {
			lock4ApplyEvent.lock();
			for (IDomainEvent<TAggregateRootId> domainEvent : domainEvents)
				internalApplyEvent(domainEvent);
		} finally {
			lock4ApplyEvent.unlock();
		}
	}

	private void handleEvent(IDomainEvent<TAggregateRootId> domainEvent){
		
		// creating new aggregate root.
		if ( this.id == null && domainEvent.getVersion() == 1 )
			this.id = domainEvent.getAggregateRootId();
		
		AggregateHandlerJavaHelper.invokeInternalHandler(this, domainEvent);
	}

	@Override
	public List<IDomainEvent<?>> getChanges() {
		ArrayList<IDomainEvent<?>> changes = new ArrayList<IDomainEvent<?>>();
		changes.addAll(uncommittedEvents);
		return changes;
	}

	@Override
	public int getChangesAmount() {
		return uncommittedEvents.size();
	}

	@Override
	public void acceptChanges(long newVersion) {
		if(version+1 != newVersion)
			throw new InvalidOperationException(String.format(
					"Cannot accept invalid version: %d, expect version: %d, current aggregateRoot type: %s, id: %s",
					newVersion, version+1, this.getClass().getName(), id.toString()
			));
		version = newVersion;
		uncommittedEvents.clear();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void replayEvents(Collection<DomainEventStream> eventStreams) {
		if( null==eventStreams || eventStreams.size()==0) return;
		for(DomainEventStream eventStream:eventStreams) {
			verifyEvent(eventStream);
			Collection<IDomainEvent<?>> events = eventStream.getEvents();
			for(IDomainEvent<?> event:events)
				handleEvent((IDomainEvent<TAggregateRootId> )event);
			version = eventStream.getVersion();
		}
	}
	
	private void appendUncommittedEvent(final IDomainEvent<TAggregateRootId> domainEvent){
		if(0<uncommittedEvents.size())
			for(IDomainEvent<?> prevousDomainEvent:uncommittedEvents)
				if(prevousDomainEvent.getClass().equals(domainEvent.getClass()))
					throw new InvalidOperationException(String.format(
							"Cannot apply duplicated domain event type: %s,current aggregateRoot type: %s, id: %s",
							domainEvent.getClass().getName(), AbstractAggregateRoot.this.getClass().getName(), id
					));
		uncommittedEvents.add(domainEvent);
	}

	private void verifyEvent(DomainEventStream eventStream){
		IAggregateRoot current = (IAggregateRoot )this;
		if (eventStream.getVersion() > 1 && eventStream.getAggregateRootId() != current.getUniqueId()) {
			throw new InvalidOperationException("Invalid domain event stream, aggregateRootId:"
					+eventStream.getAggregateRootId()
					+", expected aggregateRootId:"
					+current.getUniqueId()
					+", type:"
					+current.getClass().getName());
		}
		if (eventStream.getVersion() != current.getVersion() + 1) {
			throw new InvalidOperationException("Invalid domain event stream, version:"
					+eventStream.getVersion()
					+", expected version:"
					+current.getVersion()
					+", current aggregateRoot type:"
					+this.getClass().getName()
					+", id:"
					+current.getUniqueId());
		}
	}
	
	/**
	 * 由applyEvent和applyEvents带锁调用，请勿私自调用。
	 * @param domainEvent
	 */
	private void internalApplyEvent(IDomainEvent<TAggregateRootId> domainEvent) {
		if( null == domainEvent )
			throw new ArgumentNullException("domainEvent");
		
		if ( id == null )
			throw new RuntimeException("Aggregate Root Id can not be null!!!");
		domainEvent.setAggregateRootId( id );
		domainEvent.setVersion(version+1);
		// 聚合根响应事件
		handleEvent(domainEvent);
		// 提交事件(同时包含持久化事件和发布到q端)
		appendUncommittedEvent(domainEvent);
	}
}
