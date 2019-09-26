package pro.jiefzz.ejoker.infrastructure;

public interface ISequenceMessage extends IMessage {

	public void setAggregateRootStringId(String aggregateRootStringId);
	public String getAggregateRootStringId();
	
	public void setAggregateRootTypeName(String aggregateRootTypeName);
	public String getAggregateRootTypeName();
	
	public void setVersion(long version);
	public long getVersion();
	
}
