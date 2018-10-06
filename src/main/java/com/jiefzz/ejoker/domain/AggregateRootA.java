package com.jiefzz.ejoker.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.utils.handlerProviderHelper.containers.AggregateRootHandlerPool;
import com.jiefzz.ejoker.utils.idHelper.IDHelper;
import com.jiefzz.ejoker.z.common.ArgumentException;
import com.jiefzz.ejoker.z.common.ArgumentNullException;
import com.jiefzz.ejoker.z.common.InvalidOperationException;
import com.jiefzz.ejoker.z.common.context.annotation.persistent.PersistentIgnore;
import com.jiefzz.ejoker.z.common.utils.Ensure;

public abstract class AggregateRootA<TAggregateRootId> implements IAggregateRoot {

	private long version = 0;
	
	@PersistentIgnore
	private Queue<IDomainEvent<?>> uncommittedEvents = null;
	
	protected TAggregateRootId id = null;

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
		return null == id ? null : id.toString();
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
		if ( this.id == null && domainEvent.getVersion() == 1 ) {
			// 这里有个由String类型Id转换为实际泛型类型的id的逻辑。
//			this.id = domainEvent.getAggregateRootStringId();
			IDHelper.setAggregateRootId(this, domainEvent.getAggregateRootStringId());
			
		}
		
		AggregateRootHandlerPool.invokeInternalHandler(this, domainEvent);
	}
	
	private void appendUncommittedEvent(final IDomainEvent<TAggregateRootId> domainEvent){
		
		if(null == uncommittedEvents) {
			uncommittedEvents = new LinkedBlockingQueue<>();
		}
		

		if(0 < uncommittedEvents.size()) {
			uncommittedEvents.forEach((x) -> {

				if(x.getClass().equals(domainEvent.getClass())) {
					throw new InvalidOperationException(String.format(
							"Cannot apply duplicated domain event type: %s,current aggregateRoot type: %s, id: %s",
							domainEvent.getClass().getName(),
							AggregateRootA.this.getClass().getName(),
							id
					));
				}
				
			});
		}
		
		uncommittedEvents.offer(domainEvent);
	}

	private void verifyEvent(DomainEventStream eventStream){
		if (eventStream.getVersion() > 1 && eventStream.getAggregateRootId() != this.getUniqueId()) {
			throw new InvalidOperationException(String.format(
					"Invalid domain event stream, aggregateRootId: %s, expected aggregateRootId: %s, type: %s", 
					eventStream.getAggregateRootId(),
					this.getUniqueId(),
					this.getClass().getName()));
		}
		
		if (eventStream.getVersion() != this.getVersion() + 1l) {
			throw new InvalidOperationException(String.format(
					"Invalid domain event stream, version: %d, expected version: %d, current aggregateRoot type: %s, id:%s",
					eventStream.getVersion(),
					this.getVersion(),
					this.getClass().getName(),
					this.getUniqueId()));
		}
	}

	@Override
	public List<IDomainEvent<?>> getChanges() {
		List<IDomainEvent<?>> changes = new ArrayList<>();
		changes.addAll(uncommittedEvents);
		return changes;
	}

	@Override
	public void acceptChanges(long newVersion) {
		if(version+1 != newVersion)
			throw new InvalidOperationException(String.format(
					"Cannot accept invalid version: %d, expect version: %d, current aggregateRoot type: %s, id: %s",
					newVersion,
					version+1,
					this.getClass().getName(),
					id.toString()
			));
		version = newVersion;
		uncommittedEvents.clear();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void replayEvents(Collection<DomainEventStream> eventStreams) {
		if(null == eventStreams || 0 == eventStreams.size())
			return;
		
		for(DomainEventStream eventStream:eventStreams) {
			verifyEvent(eventStream);
			Collection<IDomainEvent<?>> events = eventStream.getEvents();
			for(IDomainEvent<?> event:events)
				handleEvent((IDomainEvent<TAggregateRootId> )event);
			version = eventStream.getVersion();
		}
	}
}
