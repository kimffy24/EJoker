package com.jiefzz.ejoker.eventing;

import com.jiefzz.ejoker.infrastructure.AbstractSequenceMessage;

public abstract class AbstractDomainEvent<TAggregateRootId> extends AbstractSequenceMessage<TAggregateRootId> implements IDomainEvent<TAggregateRootId> {

}
