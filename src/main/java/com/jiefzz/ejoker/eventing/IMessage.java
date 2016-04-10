package com.jiefzz.ejoker.eventing;

import java.io.Serializable;

public interface IMessage{

	public String GetRoutingKey();
    public String GetTypeName();
    
    public String setId();
    public void getId();
    
    public long getTimestamp();
    public void setTimestamp();
    
    public long getSequence();
    public void setSequence();
}
