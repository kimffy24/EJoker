package com.jiefzz.ejoker.infrastructure;

public interface IMessage {

	public String getRoutingKey();
	
    public String getTypeName();
    
    public void setId(String id);
    public String getId();
    
    public long getTimestamp();
    public void setTimestamp(long timestamp);
    
    public long getSequence();
    public void setSequence(long sequence);
}
