package com.jiefzz.ejoker.utils.handlerProviderHelper;

import com.jiefzz.ejoker.commanding.AbstractCommandHandler;
import com.jiefzz.ejoker.utils.handlerProviderHelper.containers.CommandHandlerPool;
import com.jiefzz.ejoker.z.common.context.annotation.assemblies.CommandHandler;
import com.jiefzz.ejoker.z.common.context.dev2.IEjokerContextDev2;

public final class RegistCommandHandlerHelper {

	static public void checkAndRegistCommandHandler(IEjokerContextDev2 ejokerContext, Class<?> clazz) {
		if(clazz.isAnnotationPresent(CommandHandler.class)) {

			CommandHandlerPool commandHandlerPool = ejokerContext.get(CommandHandlerPool.class);
			if(null == commandHandlerPool)
				ejokerContext.shallowRegister(commandHandlerPool = new CommandHandlerPool());
			
			commandHandlerPool.regist((Class<? extends AbstractCommandHandler> )clazz, () -> ejokerContext);
			
		}
	}
	
}
