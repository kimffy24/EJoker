package com.jiefzz.ejoker.infrastructure.varieties.applicationMessage;

import com.jiefzz.ejoker.infrastructure.MessageA;
import com.jiefzz.ejoker.z.common.context.annotation.persistent.PersistentIgnore;

public abstract class ApplicationMessageA extends MessageA implements IApplicationMessage {

	@PersistentIgnore
	private static final long serialVersionUID = 3053235660035704847L;

}
