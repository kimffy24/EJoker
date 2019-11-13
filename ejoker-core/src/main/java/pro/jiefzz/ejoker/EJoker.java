package pro.jiefzz.ejoker;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.domain.IAggregateRoot;
import pro.jiefzz.ejoker.domain.domainException.IDomainException;
import pro.jiefzz.ejoker.utils.domainExceptionHelper.DomainExceptionCodecHelper;
import pro.jiefzz.ejoker.utils.handlerProviderHelper.RegistCommandHandlerHelper;
import pro.jiefzz.ejoker.utils.handlerProviderHelper.RegistDomainEventHandlerHelper;
import pro.jiefzz.ejoker.utils.handlerProviderHelper.RegistMessageHandlerHelper;
import pro.jiefzz.ejoker.utils.handlerProviderHelper.containers.CommandHandlerPool;
import pro.jiefzz.ejoker.utils.handlerProviderHelper.containers.MessageHandlerPool;
import pro.jiefzz.ejoker.utils.idHelper.IDHelper;
import pro.jiefzz.ejoker.z.context.dev2.IEJokerSimpleContext;
import pro.jiefzz.ejoker.z.context.dev2.IEjokerContextDev2;
import pro.jiefzz.ejoker.z.context.dev2.impl.EjokerContextDev2Impl;

/**
 * E-Joker instance provider. E-Joker context provider.
 * @author JiefzzLon
 *
 */
public class EJoker {

	//////  properties:
	
	private final static Logger logger = LoggerFactory.getLogger(EJoker.class);
	
	public static final String SELF_PACKAGE_NAME;
	
	protected IEjokerContextDev2 context;
	
	//////  public:
	
	public IEJokerSimpleContext getEJokerContext(){
		return context;
	}

	//////  protected:
	
	protected EJoker() {
		context = new EjokerContextDev2Impl();

		final CommandHandlerPool commandAsyncHandlerPool = new CommandHandlerPool();
		((EjokerContextDev2Impl )context).shallowRegister(commandAsyncHandlerPool);
		
		final MessageHandlerPool messageHandlerPool = new MessageHandlerPool();
		((EjokerContextDev2Impl )context).shallowRegister(messageHandlerPool);
		
		// regist scanner hook
		context.registeScannerHook(clazz -> {
				RegistCommandHandlerHelper.checkAndRegistCommandAsyncHandler(clazz, commandAsyncHandlerPool, context);
				RegistMessageHandlerHelper.checkAndRegistMessageHandler(clazz, messageHandlerPool, context);
				RegistDomainEventHandlerHelper.checkAndRegistDomainEventHandler(clazz);
				
				// register StringId to GenericId codec.
				if(IAggregateRoot.class.isAssignableFrom(clazz))
					IDHelper.addAggregateRoot((Class<IAggregateRoot> )clazz);
				
				// preload IPubliashableException field inf 
				if(IDomainException.class.isAssignableFrom(clazz))
					DomainExceptionCodecHelper.getReflectFields((Class<IDomainException> )clazz);
		});
		
		context.scanPackage(SELF_PACKAGE_NAME);
		
	}
	
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
	
	public static class EJokerSingletonFactory {
		
		private final EJoker instance;
		
		public EJokerSingletonFactory(Class<? extends EJoker> prototype) {
			
			Method declaredMethod = null;
			try {
				declaredMethod = prototype.getDeclaredMethod("prepareStatic");
			} catch (NoSuchMethodException
					| SecurityException
					| IllegalArgumentException e) {
				;
			}
			if(null != declaredMethod) {
				declaredMethod.setAccessible(true);
				try {
					declaredMethod.invoke(null);
				} catch (IllegalAccessException
						| IllegalArgumentException
						| InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			}
			
			try {
				Constructor<? extends EJoker> constructor = prototype.getDeclaredConstructor();
				constructor.setAccessible(true);
				instance = constructor.newInstance();
			} catch (NoSuchMethodException
					| SecurityException
					| InstantiationException
					| IllegalAccessException
					| IllegalArgumentException
					| InvocationTargetException e) {
				throw new RuntimeException(e);
			}
			
		}

		public EJoker getInstance(){
			return instance;
		}
	}
}