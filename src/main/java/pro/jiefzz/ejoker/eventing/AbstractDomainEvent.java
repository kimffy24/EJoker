package pro.jiefzz.ejoker.eventing;

import pro.jiefzz.ejoker.infrastructure.AbstractSequenceMessage;

public abstract class AbstractDomainEvent<TAggregateRootId> extends AbstractSequenceMessage<TAggregateRootId> implements IDomainEvent<TAggregateRootId> {

}
