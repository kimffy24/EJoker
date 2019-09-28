package pro.jiefzz.ejoker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.domain.IAggregateRoot;
import pro.jiefzz.ejoker.infrastructure.messaging.varieties.publishableException.IPublishableException;
import pro.jiefzz.ejoker.utils.handlerProviderHelper.RegistCommandAsyncHandlerHelper;
import pro.jiefzz.ejoker.utils.handlerProviderHelper.RegistCommandHandlerHelper;
import pro.jiefzz.ejoker.utils.handlerProviderHelper.RegistDomainEventHandlerHelper;
import pro.jiefzz.ejoker.utils.handlerProviderHelper.RegistMessageHandlerHelper;
import pro.jiefzz.ejoker.utils.idHelper.IDHelper;
import pro.jiefzz.ejoker.utils.publishableExceptionHelper.PublishableExceptionCodecHelper;
import pro.jiefzz.ejoker.z.context.dev2.IEJokerSimpleContext;
import pro.jiefzz.ejoker.z.context.dev2.IEjokerContextDev2;
import pro.jiefzz.ejoker.z.context.dev2.impl.EjokerContextDev2Impl;

/**
 * E-Joker instance provider. E-Joker context provider.
 * @author JiefzzLon
 *
 */
public class EJoker {
	
	private final static Logger logger = LoggerFactory.getLogger(EJoker.class);
	
	//////  public:
	
	public static final String SELF_PACKAGE_NAME;
	
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
//				if(!clazz.getPackage().getName().startsWith(SELF_PACKAGE_NAME)) {
					// We make sure that CommandHandler and DomainEventHandler will not in E-Joker Framework package.
					RegistCommandHandlerHelper.checkAndRegistCommandHandler(context, clazz);
					RegistCommandAsyncHandlerHelper.checkAndRegistCommandAsyncHandler(context, clazz);
					RegistDomainEventHandlerHelper.checkAndRegistDomainEventHandler(clazz);
//				}
				RegistMessageHandlerHelper.checkAndRegistMessageHandler(context, clazz);
				
				// register StringId to GenericId codec.
				if(IAggregateRoot.class.isAssignableFrom(clazz))
					IDHelper.addAggregateRoot((Class<IAggregateRoot> )clazz);
				
				// preload IPubliashableException field inf 
				if(IPublishableException.class.isAssignableFrom(clazz))
					PublishableExceptionCodecHelper.getReflectFields((Class<IPublishableException> )clazz);
		});
		
		context.scanPackage(SELF_PACKAGE_NAME);
		
	}

	//////  properties:
	
	protected static EJoker instance;
	
	protected IEjokerContextDev2 context;
	
	static {
		String packageName = EjokerContextDev2Impl.class.getName();
		String[] split = packageName.split("\\.");
		SELF_PACKAGE_NAME = split[0]
				+ "."
				+ split[1]
				+ "."
				+ split[2];
		logger.debug("SELF_PACNAGE_NAME = {}", SELF_PACKAGE_NAME);
		
	}
	
}
