package com.jiefzz.ejoker.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.utils.handlerProviderHelper.containers.AggregateRootHandlerPool;
import com.jiefzz.ejoker.z.common.ArgumentException;
import com.jiefzz.ejoker.z.common.ArgumentNullException;
import com.jiefzz.ejoker.z.common.InvalidOperationException;
import com.jiefzz.ejoker.z.common.context.annotation.persistent.PersistentIgnore;
import com.jiefzz.ejoker.z.common.utils.Ensure;

public abstract class AggregateRootA<TAggregateRootId> implements IAggregateRoot {

	private long version = 0;
	
	private Lock lock4ApplyEvent = new ReentrantLock();

	@PersistentIgnore
	private Queue<IDomainEvent<?>> uncommittedEvents = null;
	
	private TAggregateRootId id = null;

	public TAggregateRootId getId(){
		return id;
	}

	protected void setId(TAggregateRootId id) {
		this.id = id;
	}
	
	protected AggregateRootA(){ }

	protected AggregateRootA(TAggregateRootId id) {
		this();
		if (id == null)
			throw new ArgumentNullException("id");
		this.id = id;
	}

	protected AggregateRootA(TAggregateRootId id, long version) {
		this(id);
		if (version < 0)
			throw new ArgumentException(String.format("Version can not small than 0, aggregateRootId=%s version=%s", id, version));
		this.version = version;
	}

	@Override
	public String getUniqueId() {
		return null==id?null:id.toString();
	}

	@Override
	public long getVersion() {
		return version;
	}
	
	protected void applyEvent(IDomainEvent<TAggregateRootId> domainEvent) {
		
		Ensure.notNull(domainEvent, "domainEvent");
		if (null == this.id) {
            throw new RuntimeException("Aggregate root id cannot be null.");
        }
		
		domainEvent.setAggregateRootId(this.id);
		domainEvent.setVersion(this.version + 1);

		// 聚合根响应事件
		handleEvent(domainEvent);
		// 提交事件(同时包含持久化事件和发布到q端)
		appendUncommittedEvent(domainEvent);
	}
	
	protected void applyEvents(IDomainEvent<TAggregateRootId>[] domainEvents) {
		for(IDomainEvent<TAggregateRootId> event : domainEvents)
			applyEvent(event);
	}

	private void handleEvent(IDomainEvent<TAggregateRootId> domainEvent){
		
		// creating new aggregate root.
		if ( this.id == null && domainEvent.getVersion() == 1 )
			this.id = domainEvent.getAggregateRootId();
		
		AggregateRootHandlerPool.invokeInternalHandler(this, domainEvent);
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
							domainEvent.getClass().getName(), AggregateRootA.this.getClass().getName(), id
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
}
