package pro.jk.ejoker;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import pro.jk.ejoker.common.context.dev2.EjokerRootDefinationStore;
import pro.jk.ejoker.common.context.dev2.IEJokerSimpleContext;
import pro.jk.ejoker.common.context.dev2.IEjokerContextDev2;
import pro.jk.ejoker.common.context.dev2.impl.EjokerContextDev2Impl;
import pro.jk.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jk.ejoker.domain.IAggregateRoot;
import pro.jk.ejoker.domain.domainException.IDomainException;
import pro.jk.ejoker.utils.domainExceptionHelper.DomainExceptionCodecHelper;
import pro.jk.ejoker.utils.handlerProviderHelper.RegistCommandHandlerHelper;
import pro.jk.ejoker.utils.handlerProviderHelper.RegistDomainEventHandlerHelper;
import pro.jk.ejoker.utils.handlerProviderHelper.RegistMessageHandlerHelper;
import pro.jk.ejoker.utils.handlerProviderHelper.containers.CommandHandlerPool;
import pro.jk.ejoker.utils.handlerProviderHelper.containers.MessageHandlerPool;
import pro.jk.ejoker.utils.idHelper.IDHelper;

/**
 * E-Joker instance provider. E-Joker context provider.
 * @author JiefzzLon
 *
 */
public class EJoker {

	//////  properties:
	
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
		
		SELF_PACKAGE_NAME = EjokerRootDefinationStore.SELF_PACKAGE_NAME;
		
		SystemAsyncHelper.setDefaultPoolSize(EJokerEnvironment.ASYNC_INTERNAL_EXECUTE_THREADPOOL_SIZE);
		
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
