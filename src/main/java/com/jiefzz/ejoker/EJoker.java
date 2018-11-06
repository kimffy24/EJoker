package com.jiefzz.ejoker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.IPublishableException;
import com.jiefzz.ejoker.utils.handlerProviderHelper.RegistCommandHandlerHelper;
import com.jiefzz.ejoker.utils.handlerProviderHelper.RegistDomainEventHandlerHelper;
import com.jiefzz.ejoker.utils.handlerProviderHelper.RegistMessageHandlerHelper;
import com.jiefzz.ejoker.utils.idHelper.IDHelper;
import com.jiefzz.ejoker.utils.publishableExceptionHelper.PublishableExceptionCodecHelper;
import com.jiefzz.ejoker.z.common.context.dev2.IEJokerSimpleContext;
import com.jiefzz.ejoker.z.common.context.dev2.IEjokerContextDev2;
import com.jiefzz.ejoker.z.common.context.dev2.impl.EjokerContextDev2Impl;

/**
 * E-Joker instance provider. E-Joker context provider.
 * @author JiefzzLon
 *
 */
public class EJoker {
	
	private final static Logger logger = LoggerFactory.getLogger(EJoker.class);
	
	//////  public:
	
	public static final String SELF_PACNAGE_NAME;
	
	public static EJoker getInstance(){
		if ( instance == null )
			instance = new EJoker();
		return instance;
	}
	
	public IEJokerSimpleContext getEJokerContext(){
		return context;
	}

	//////  private:
	
	protected EJoker() {
		context = new EjokerContextDev2Impl();
		
		// regist scanner hook
		context.registeScannerHook(clazz -> {
				if(!clazz.getPackage().getName().startsWith(SELF_PACNAGE_NAME)) {
					// We make sure that CommandHandler and DomainEventHandler will not in E-Joker Framework package.
					RegistCommandHandlerHelper.checkAndRegistCommandHandler(clazz);
					RegistDomainEventHandlerHelper.checkAndRegistDomainEventHandler(clazz);
				}
				RegistMessageHandlerHelper.checkAndRegistMessageHandler(clazz);
				
				// register StringId to GenericId codec.
				if(clazz.isAssignableFrom(IAggregateRoot.class))
					IDHelper.addAggregateRoot((Class<IAggregateRoot> )clazz);
				
				// preload IPubliashableException field inf 
				if(clazz.isAssignableFrom(IPublishableException.class))
					PublishableExceptionCodecHelper.getReflectFields((Class<IPublishableException> )clazz);
		});
		
		context.scanPackage(SELF_PACNAGE_NAME);
		
	}

	//////  properties:
	
	protected static EJoker instance;
	
	protected IEjokerContextDev2 context;
	
	static {
		String packageName = EjokerContextDev2Impl.class.getName();
		String[] split = packageName.split("\\.");
		SELF_PACNAGE_NAME = split[0]
				+ "."
				+ split[1]
				+ "."
				+ split[2];
		logger.debug("SELF_PACNAGE_NAME = {}", SELF_PACNAGE_NAME);
	}
	
}
