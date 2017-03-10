package com.jiefzz.ejoker;

import com.jiefzz.ejoker.utils.handlerProviderHelper.RegistCommandHandlerHelper;
import com.jiefzz.ejoker.utils.handlerProviderHelper.RegistDomainEventHandlerHelper;
import com.jiefzz.ejoker.utils.handlerProviderHelper.RegistMessageHandlerHelper;
import com.jiefzz.ejoker.z.common.context.IEJokerContext;
import com.jiefzz.ejoker.z.common.context.IEJokerSimpleContext;
import com.jiefzz.ejoker.z.common.context.IEjokerClassScanHook;
import com.jiefzz.ejoker.z.common.context.impl.DefaultEJokerContext;

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
		context = new DefaultEJokerContext();
		
		// regist scanner hook
		context.registeScanHook(new IEjokerClassScanHook() {
			@Override
			public void accept(Class<?> clazz) {
				if(!clazz.getPackage().getName().startsWith(SELF_PACNAGE_NAME)) {
					// We make sure that CommandHandler and DomainEventHandler will not in E-Joker Framework package.
					RegistCommandHandlerHelper.checkAndRegistCommandHandler(clazz);
					RegistDomainEventHandlerHelper.checkAndRegistDomainEventHandler(clazz);
				}
				RegistMessageHandlerHelper.checkAndRegistMessageHandler(clazz);
			}
		});
		
		context.scanPackageClassMeta(SELF_PACNAGE_NAME);
	}

	//////  properties:
	
	private static EJoker instance;
	private IEJokerContext context;
	
}
