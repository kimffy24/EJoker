package com.jiefzz.ejoker.eventing;

import com.jiefzz.ejoker.infrastructure.SequenceMessageAbstract;

public abstract class DomainEventAbstract<TAggregateRootId> extends SequenceMessageAbstract<TAggregateRootId> implements IDomainEvent<TAggregateRootId> {

}
