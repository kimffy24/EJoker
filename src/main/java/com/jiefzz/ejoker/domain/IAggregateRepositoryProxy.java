package com.jiefzz.ejoker.domain;

import com.jiefzz.ejoker.infrastructure.IObjectProxy;

public interface IAggregateRepositoryProxy extends IObjectProxy {

	@SuppressWarnings("rawtypes")
	public IAggregateRoot get(String aggregateRootId);
	
}
