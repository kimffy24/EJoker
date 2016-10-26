package com.jiefzz.ejoker.infrastructure;

import java.io.Serializable;

public interface IMessage extends Serializable{

	public String getRoutingKey();
    public String getTypeName();
    
    public void setId(String id);
    public String getId();
    
    public long getTimestamp();
    public void setTimestamp(long timestamp);
    
    public long getSequence();
    public void setSequence(long sequence);
}
