package com.jiefzz.ejoker.commanding.impl;

import com.jiefzz.ejoker.commanding.ICommandContext;
import com.jiefzz.ejoker.domain.IAggregateRoot;

public class CommandContextImpl implements ICommandContext {

	@Override
	public void add(IAggregateRoot<?> aggregateRoot) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T extends IAggregateRoot<?>> T get(Object id, Boolean tryFromCache) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends IAggregateRoot<?>> T get(Object id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setResult(String result) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getResult() {
		// TODO Auto-generated method stub
		return null;
	}

}
