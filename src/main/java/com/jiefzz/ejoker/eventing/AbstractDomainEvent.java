package com.jiefzz.ejoker.eventing;

import com.jiefzz.ejoker.infrastructure.AbstractSequenceMessage;
import com.jiefzz.ejoker.infrastructure.z.common.context.annotation.persistent.PersistentIgnore;

public abstract class AbstractDomainEvent<TAggregateRootId> extends AbstractSequenceMessage<TAggregateRootId> implements IDomainEvent<TAggregateRootId> {
	@PersistentIgnore
	private static final long serialVersionUID = -2388063710077017716L;
}
