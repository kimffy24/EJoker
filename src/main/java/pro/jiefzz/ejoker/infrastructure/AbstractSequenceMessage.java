package pro.jiefzz.ejoker.infrastructure;

public abstract class AbstractSequenceMessage<TAggregateRootId> extends AbstractMessage
	implements ISequenceMessage {

	private TAggregateRootId aggregateRootId;
	
	private String aggregateRootStringId;
	
	private String aggregateRootTypeName;
	
	private long version;

	public void setAggregateRootId(TAggregateRootId aggregateRootId) {
		this.aggregateRootId = aggregateRootId;
		this.aggregateRootStringId = aggregateRootId.toString();
	}

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
	public String getRoutingKey() {
		return aggregateRootStringId;
	}
}
