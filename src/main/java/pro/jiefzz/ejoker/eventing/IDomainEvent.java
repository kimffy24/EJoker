package pro.jiefzz.ejoker.eventing;

import pro.jiefzz.ejoker.infrastructure.messaging.IMessage;

/**
 * 
 * Enode中分了带泛型的IDomainEvent<>和不带泛型的IDomainEvent，这里不做区分。
 * 
 * @author kimffy
 *
 * @param <TAggregateRootId>
 */
public interface IDomainEvent<TAggregateRootId> extends IMessage {
	
	public void setCommandId(String commandId);
	public String getCommandId();

	public void setAggregateRootId(TAggregateRootId aggregateRootId);
	public TAggregateRootId getAggregateRootId();
	
	public void setAggregateRootStringId(String aggregateRootStringId);
	public String getAggregateRootStringId();
	
	public void setAggregateRootTypeName(String aggregateRootTypeName);
	public String getAggregateRootTypeName();
	
	public void setVersion(long version);
	public long getVersion();
	
	public void setSpecVersion(long specVersion);
	public long getSpecVersion();

	public void setSequence(int sequence);
	public int getSequence();
	
}