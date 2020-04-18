package pro.jk.ejoker.eventing;

import pro.jk.ejoker.messaging.AbstractMessage;

public abstract class AbstractDomainEvent<TAggregateRootId> extends AbstractMessage implements IDomainEvent<TAggregateRootId> {

	private String commandId;
	
	private TAggregateRootId aggregateRootId;
	
	private String aggregateRootStringId;
	
	private String aggregateRootTypeName;
	
	private long version;
	
	private long specVersion;
	
	private int sequence;
	
	public AbstractDomainEvent() {
		super();
		version = 1l;
		specVersion = 1l;
		sequence = 1;
	}

	@Override
	public void setAggregateRootId(TAggregateRootId aggregateRootId) {
		this.aggregateRootId = aggregateRootId;
		this.aggregateRootStringId = aggregateRootId.toString();
	}

	@Override
	public TAggregateRootId getAggregateRootId() {
		return this.aggregateRootId;
	}

	@Override
	public void setAggregateRootStringId(String aggregateRootStringId) {
		this.aggregateRootStringId = aggregateRootStringId;
	}

	@Override
	public String getAggregateRootStringId() {
		return aggregateRootStringId;
	}

	@Override
	public void setAggregateRootTypeName(String aggregateRootTypeName) {
		this.aggregateRootTypeName = aggregateRootTypeName;
	}

	@Override
	public String getAggregateRootTypeName() {
		return aggregateRootTypeName;
	}

	@Override
	public void setVersion(long version) {
		this.version = version;
	}

	@Override
	public long getVersion() {
		return version;
	}

	@Override
	public void setCommandId(String commandId) {
		this.commandId = commandId;
	}

	@Override
	public String getCommandId() {
		return commandId;
	}

	@Override
	public void setSpecVersion(long specVersion) {
		this.specVersion = specVersion;
	}

	@Override
	public long getSpecVersion() {
		return specVersion;
	}

	@Override
	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	@Override
	public int getSequence() {
		return sequence;
	}
	
}
