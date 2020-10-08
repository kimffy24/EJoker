package pro.jk.ejoker.messaging;

import java.util.Map;

public interface IMessage {

	public void setId(String id);
    public String getId();
    
    public long getTimestamp();
    public void setTimestamp(long timestamp);
    
    public Map<String, String> getItems();
    public void setItems(Map<String, String> items);
    
    public void mergeItems(Map<String, String> items);
    
}
