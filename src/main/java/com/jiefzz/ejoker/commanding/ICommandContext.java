package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.domain.IAggregateRoot;

public interface ICommandContext {

    public void add(IAggregateRoot<?> aggregateRoot);
    
    public <T extends IAggregateRoot<?>> T get(Object id, Boolean tryFromCache);
    public <T extends IAggregateRoot<?>> T get(Object id);
    
    void setResult(String result);
    String getResult();
    
}
