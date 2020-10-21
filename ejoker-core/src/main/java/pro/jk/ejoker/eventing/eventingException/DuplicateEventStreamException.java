package pro.jk.ejoker.eventing.eventingException;

import static pro.jk.ejoker.common.system.enhance.StringUtilx.fmt;

import pro.jk.ejoker.eventing.DomainEventStream;

public class DuplicateEventStreamException extends RuntimeException {

	private static final long serialVersionUID = 4422312728014737681L;

	private final static String msgTpl =
			"Aggregate root event stream already exist in the EventCommittingContextMailBox!!! [type:{}, id:{}, eventStreamId: {}]";
	
	public final DomainEventStream domainEventStream;

	public DuplicateEventStreamException(DomainEventStream domainEventStream) {
		super(fmt(msgTpl, domainEventStream.getAggregateRootTypeName(), domainEventStream.getAggregateRootId(), domainEventStream.getVersion()));
		this.domainEventStream = domainEventStream;
	}
	
}
