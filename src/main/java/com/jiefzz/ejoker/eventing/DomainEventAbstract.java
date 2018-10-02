package com.jiefzz.ejoker.eventing;

import com.jiefzz.ejoker.infrastructure.SequenceMessageA;

public abstract class DomainEventAbstract<TAggregateRootId> extends SequenceMessageA<TAggregateRootId> implements IDomainEvent<TAggregateRootId> {

}
