package com.jiefzz.ejoker;

import com.jiefzz.ejoker.z.common.context.IContext;
import com.jiefzz.ejoker.z.common.context.IContextWorker;
import com.jiefzz.ejoker.z.common.context.impl.InstanceContainer;
import com.jiefzz.ejoker.z.common.scavenger.Scavenger;

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
	
	private IContextWorker initializeEJoker(){
		context = new InstanceContainer();
		context.annotationScan("com.jiefzz.ejoker");
		return context;
	}

	private IContextWorker context;
	
}
