package com.jiefzz.ejoker;

import com.jiefzz.ejoker.utils.handlerProviderHelper.RegistCommandHandlerHelper;
import com.jiefzz.ejoker.utils.handlerProviderHelper.RegistDomainEventHandlerHelper;
import com.jiefzz.ejoker.utils.handlerProviderHelper.RegistMessageHandlerHelper;
import com.jiefzz.ejoker.z.common.context.dev2.IEJokerSimpleContext;
import com.jiefzz.ejoker.z.common.context.dev2.IEjokerContextDev2;
import com.jiefzz.ejoker.z.common.context.dev2.impl.EjokerContextDev2Impl;

/**
 * E-Joker instance provider. E-Joker context provider.
 * @author JiefzzLon
 *
 */
public class EJoker {
	
	//////  public:
	
	public static final String SELF_PACNAGE_NAME = "com.jiefzz.ejoker";
	
	public static EJoker getInstance(){
		if ( instance == null )
			instance = new EJoker();
		return instance;
	}
	
	public IEJokerSimpleContext getEJokerContext(){
		return context;
	}

	//////  private:
	
	private EJoker() {
		context = new EjokerContextDev2Impl();
		
		// regist scanner hook
		context.registeScannerHook(clazz -> {
				if(!clazz.getPackage().getName().startsWith(SELF_PACNAGE_NAME)) {
					// We make sure that CommandHandler and DomainEventHandler will not in E-Joker Framework package.
					RegistCommandHandlerHelper.checkAndRegistCommandHandler(clazz);
					RegistDomainEventHandlerHelper.checkAndRegistDomainEventHandler(clazz);
				}
				RegistMessageHandlerHelper.checkAndRegistMessageHandler(clazz);
		});
		
		context.scanPackage(SELF_PACNAGE_NAME);
	}

	//////  properties:
	
	private static EJoker instance;
	private IEjokerContextDev2 context;
	
}
