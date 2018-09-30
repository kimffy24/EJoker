package com.jiefzz.ejoker.infrastructure.varieties.applicationMessage;

import com.jiefzz.ejoker.infrastructure.MessageAbstract;
import com.jiefzz.ejoker.z.common.context.annotation.persistent.PersistentIgnore;

public abstract class AbstractApplicationMessage extends MessageAbstract implements IApplicationMessage {

	@PersistentIgnore
	private static final long serialVersionUID = 3053235660035704847L;

}
