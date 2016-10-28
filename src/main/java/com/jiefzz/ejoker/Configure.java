package com.jiefzz.ejoker;

import com.jiefzz.ejoker.z.common.context.IEJokerContext;
import com.jiefzz.ejoker.z.common.context.IEJokerSimpleContext;
import com.jiefzz.ejoker.z.common.context.impl.DefaultEJokerContext;

public class Configure {
	
	private static Configure instance;
	
	public static Configure getInstance(){
		if ( instance == null )
			instance = new Configure();
		return instance;
	}
	
	private Configure(){ 
		initializeEJoker();
	}
	
	public IEJokerSimpleContext getEJokerContext(){
		return context;
	}
	
	private IEJokerContext initializeEJoker(){
		context = new DefaultEJokerContext();
		context.scanPackageClassMeta("com.jiefzz.ejoker");
		return context;
	}

	private IEJokerContext context;
	
}
