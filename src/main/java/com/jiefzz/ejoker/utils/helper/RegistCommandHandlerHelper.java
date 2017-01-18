package com.jiefzz.ejoker.utils.helper;

import com.jiefzz.ejoker.commanding.AbstractCommandHandler;
import com.jiefzz.ejoker.commanding.helper.CommandHandlerJavaHelper;
import com.jiefzz.ejoker.z.common.context.annotation.assemblies.CommandHandler;

public final class RegistCommandHandlerHelper {

	static public void checkAndRegistCommandHandler(Class<?> clazz) {
		if(clazz.isAnnotationPresent(CommandHandler.class))
			CommandHandlerJavaHelper.regist((Class<? extends AbstractCommandHandler> )clazz);
	}
	
}
