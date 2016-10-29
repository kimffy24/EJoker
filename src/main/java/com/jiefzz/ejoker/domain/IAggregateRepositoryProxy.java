package com.jiefzz.ejoker.domain;

import com.jiefzz.ejoker.infrastructure.IObjectProxy;

public interface IAggregateRepositoryProxy extends IObjectProxy {

	public IAggregateRoot get(String aggregateRootId);
	
}
