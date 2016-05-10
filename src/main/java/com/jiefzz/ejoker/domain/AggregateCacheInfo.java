package com.jiefzz.ejoker.domain;

public class AggregateCacheInfo {

	public IAggregateRoot aggregateRoot;
    public long lastUpdateTime;

    public AggregateCacheInfo(IAggregateRoot aggregateRoot) {
        this.aggregateRoot = aggregateRoot;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public boolean isExpired(int timeoutSeconds) {
    	//取秒数？？
        return (System.currentTimeMillis() - lastUpdateTime)%1000 >= timeoutSeconds;
    }
    
}
