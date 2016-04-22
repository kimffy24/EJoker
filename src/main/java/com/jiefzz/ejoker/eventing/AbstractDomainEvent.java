package com.jiefzz.ejoker.eventing;

import com.jiefzz.ejoker.infrastructure.AbstractSequenceMessage;

public abstract class AbstractDomainEvent<TAggregateRootId> extends AbstractSequenceMessage<TAggregateRootId> implements IDomainEvent<TAggregateRootId> {
	private static final long serialVersionUID = -2388063710077017716L;
}
