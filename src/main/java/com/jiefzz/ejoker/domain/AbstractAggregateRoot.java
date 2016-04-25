package com.jiefzz.ejoker.domain;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import com.jiefzz.ejoker.annotation.context.Dependence;
import com.jiefzz.ejoker.annotation.persistent.PersistentIgnore;
import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.infrastructure.ArgumentNullException;

public abstract class AbstractAggregateRoot<TAggregateRootId> implements IAggregateRoot<TAggregateRootId> {

	@PersistentIgnore
	private static final long serialVersionUID = -1803833835739801207L;

	@PersistentIgnore
    private static final LinkedHashSet<IDomainEvent> emptyEvents = new LinkedHashSet<IDomainEvent>();

	// TODO Please use your IoC to inject this Implement Object!!!
	@Dependence
	@PersistentIgnore
	IAggregateRootInternalHandlerProvider eventHandlerProvider;
	
	
	private long version=0;
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
		this.version = version;
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
		return version;
	}
	
	public <TRole> TRole actAs(Class<TRole> clazz) {
		if ( !clazz.isInterface() ) throw new RuntimeException("This class could not act as ["+clazz.getName()+"]");
		if ( !clazz.isAssignableFrom(this.getClass())) throw new RuntimeException("This class could not act as ["+clazz.getName()+"]");
		return (TRole) this;
	}

	protected void ApplyEvent(IDomainEvent<TAggregateRootId> domainEvent) {

		if ( _id == null )
			throw new IllegalAggregateRootIdException("Domain Aggregate Id is null!!!");
		domainEvent.setAggregateRootId( _id );
		domainEvent.setVersion(version+1);
		// 接收事件
		HandleEvent(domainEvent);
		// 提交事件到队列
		AppendUncommittedEvent(domainEvent);
	}
	protected void ApplyEvents(IDomainEvent<TAggregateRootId>[] domainEvents) {
		for (IDomainEvent<TAggregateRootId> domainEvent : domainEvents)
			ApplyEvent(domainEvent);
	}

	private void HandleEvent(IDomainEvent<TAggregateRootId> domainEvent){
		if (eventHandlerProvider == null)
			throw new InvalidOperationException("IAggregateRootInternalHandlerProvider was never inject before!!!");
		DelegateAction<IAggregateRoot, IDomainEvent> handler = (DelegateAction<IAggregateRoot, IDomainEvent>) eventHandlerProvider.getInnerEventHandler(this.getClass(), domainEvent.getClass());
		if (handler == null) throw new InvalidOperationException("Could not found event handler for " 
					+ domainEvent.getClass().getName() + " of " + this.getClass().getName() );
		
		// TODO 第一次使用
		if ( this._id == null && domainEvent.getVersion() == 1 )
			this._id = domainEvent.getAggregateRootId();
		
		handler.delegate(this, domainEvent);;
		
	}

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
	private void AppendUncommittedEvent(IDomainEvent<TAggregateRootId> domainEvent){}

	private void verifyEvent(DomainEventStream eventStream){
		IAggregateRoot<TAggregateRootId> current = (IAggregateRoot<TAggregateRootId>)this;
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
