package pro.jiefzz.ejoker.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import pro.jiefzz.ejoker.eventing.DomainEventStream;
import pro.jiefzz.ejoker.eventing.IDomainEvent;
import pro.jiefzz.ejoker.utils.handlerProviderHelper.containers.AggregateRootHandlerPool;
import pro.jiefzz.ejoker.utils.idHelper.IDHelper;
import pro.jiefzz.ejoker.z.context.annotation.persistent.PersistentIgnore;
import pro.jiefzz.ejoker.z.exceptions.ArgumentException;
import pro.jiefzz.ejoker.z.exceptions.ArgumentNullException;
import pro.jiefzz.ejoker.z.exceptions.InvalidOperationException;
import pro.jiefzz.ejoker.z.utils.Ensure;

public abstract class AbstractAggregateRoot<TAggregateRootId> implements IAggregateRoot {

	@PersistentIgnore
	private List<IDomainEvent<?>> uncommittedEvents = null;
	
	private TAggregateRootId id = null;

	private long version = 0;
	
	public TAggregateRootId getId(){
		return id;
	}

	protected void setId(TAggregateRootId id) {
		this.id = id;
	}
	
	protected AbstractAggregateRoot(){ }

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
		domainEvent.setAggregateRootStringId(this.getUniqueId());
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
			// this.id = domainEvent.getAggregateRootStringId();
			IDHelper.setAggregateRootId(this, domainEvent.getAggregateRootStringId());
			
		}
		
		AggregateRootHandlerPool.invokeInternalHandler(this, domainEvent);
	}
	
	private void appendUncommittedEvent(final IDomainEvent<TAggregateRootId> domainEvent){
		
		if(null == uncommittedEvents) {
			uncommittedEvents = new ArrayList<>();
		}
		

		if(0 < uncommittedEvents.size()) {
			Class<?> eType = domainEvent.getClass();
			for(IDomainEvent<?> x : uncommittedEvents) {
				if(eType.equals(x.getClass())) {
					throw new InvalidOperationException(String.format(
							"Cannot apply duplicated domain event type: %s, current aggregateRoot type: %s, id: %s",
							eType.getName(),
							AbstractAggregateRoot.this.getClass().getName(),
							id
					));
				}
			}
		}
		
		uncommittedEvents.add(domainEvent);
		
	}

	private void verifyEvent(DomainEventStream eventStream){
		if (eventStream.getVersion() > 1 &&  !this.getUniqueId().equals(eventStream.getAggregateRootId())) {
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
		if(null != uncommittedEvents)
			changes.addAll(uncommittedEvents);
		return changes;
	}

	@Override
	public void acceptChanges() {
		if (null != uncommittedEvents && !uncommittedEvents.isEmpty()) {
			version = uncommittedEvents.get(0).getVersion();
			uncommittedEvents.clear();
		}
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
