package com.jiefzz.ejoker;

import com.jiefzz.ejoker.z.common.context.IContext;
import com.jiefzz.ejoker.z.common.context.impl.DefaultContext;

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
	
	public IContext getEJokerContext(){
		return context;
	}
	
	private DefaultContext initializeEJoker(){
		context = new DefaultContext();
		context.annotationScan("com.jiefzz.ejoker");
		return context;
	}

	private DefaultContext context;
	
}
