package com.jiefzz.ejoker.utils.handlerProviderHelper;

import com.jiefzz.ejoker.commanding.AbstractCommandHandler;
import com.jiefzz.ejoker.utils.handlerProviderHelper.containers.CommandHandlerPool;
import com.jiefzz.ejoker.z.common.context.annotation.assemblies.CommandHandler;

public final class RegistCommandHandlerHelper {

	static public void checkAndRegistCommandHandler(Class<?> clazz) {
		if(clazz.isAnnotationPresent(CommandHandler.class))
			CommandHandlerPool.regist((Class<? extends AbstractCommandHandler> )clazz);
	}
	
}
