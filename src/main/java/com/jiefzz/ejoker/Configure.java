package com.jiefzz.ejoker;

import com.jiefzz.ejoker.z.common.context.IEjokerStandardContext;
import com.jiefzz.ejoker.z.common.context.impl.EjokerContextImpl;

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
	
	public IEjokerStandardContext getEJokerContext(){
		return context;
	}
	
	private IEjokerStandardContext initializeEJoker(){
		context = new EjokerContextImpl();
		context.annotationScan("com.jiefzz.ejoker");
		return context;
	}

	private IEjokerStandardContext context;
	
}
