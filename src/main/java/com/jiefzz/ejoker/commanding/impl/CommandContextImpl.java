package com.jiefzz.ejoker.commanding.impl;

import com.jiefzz.ejoker.commanding.ICommandContext;
import com.jiefzz.ejoker.domain.IAggregateRoot;

public class CommandContextImpl implements ICommandContext {

	@Override
	public void Add(IAggregateRoot aggregateRoot) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T extends IAggregateRoot> T Get(Object id, Boolean tryFromCache) {
		return null;
	}

	@Override
	public <T extends IAggregateRoot> T Get(Object id) {
		return Get(id, true);
	}

	@Override
	public void SetResult(String result) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String GetResult() {
		// TODO Auto-generated method stub
		return null;
	}

}
