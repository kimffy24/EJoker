package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.domain.IAggregateRoot;

public interface ICommandContext {

    public void Add(IAggregateRoot aggregateRoot);
    
    public <T extends IAggregateRoot> T Get(Object id, Boolean tryFromCache);
    public <T extends IAggregateRoot> T Get(Object id);
    
    void SetResult(String result);
    String GetResult();
    
}
