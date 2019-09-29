package pro.jiefzz.ejoker.utils.handlerProviderHelper;

import pro.jiefzz.ejoker.commanding.AbstractCommandHandler;
import pro.jiefzz.ejoker.utils.handlerProviderHelper.containers.CommandAsyncHandlerPool;
import pro.jiefzz.ejoker.z.context.annotation.assemblies.CommandHandler;
import pro.jiefzz.ejoker.z.context.dev2.IEjokerContextDev2;

public final class RegistCommandAsyncHandlerHelper {

	static public void checkAndRegistCommandAsyncHandler(Class<?> clazz, CommandAsyncHandlerPool commandAsyncHandlerPool, IEjokerContextDev2 ejokerContext) {

		if(clazz.isAnnotationPresent(CommandHandler.class)) {
			commandAsyncHandlerPool.regist((Class<? extends AbstractCommandHandler> )clazz, () -> ejokerContext);
		}
		
	}
	
}
