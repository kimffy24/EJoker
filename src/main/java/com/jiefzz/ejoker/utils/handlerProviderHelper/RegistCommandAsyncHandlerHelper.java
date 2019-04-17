package com.jiefzz.ejoker.utils.handlerProviderHelper;

import com.jiefzz.ejoker.commanding.AbstractCommandHandler;
import com.jiefzz.ejoker.utils.handlerProviderHelper.containers.CommandAsyncHandlerPool;
import com.jiefzz.ejoker.z.common.context.annotation.assemblies.CommandHandler;
import com.jiefzz.ejoker.z.common.context.dev2.IEjokerContextDev2;

public final class RegistCommandAsyncHandlerHelper {

	static public void checkAndRegistCommandAsyncHandler(IEjokerContextDev2 ejokerContext, Class<?> clazz) {
		CommandAsyncHandlerPool commandAsyncHandlerPool = ejokerContext.get(CommandAsyncHandlerPool.class);
		if(null == commandAsyncHandlerPool)
			ejokerContext.shallowRegister(commandAsyncHandlerPool = new CommandAsyncHandlerPool());

		if(clazz.isAnnotationPresent(CommandHandler.class)) {
			
			commandAsyncHandlerPool.regist((Class<? extends AbstractCommandHandler> )clazz, () -> ejokerContext);
			
		}
	}
	
}
