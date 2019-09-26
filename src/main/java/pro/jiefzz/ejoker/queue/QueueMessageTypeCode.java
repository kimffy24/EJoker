package pro.jiefzz.ejoker.queue;

public enum QueueMessageTypeCode {

	/**
	 * unuse 0
	 */
	UnsetTypeCode,
	
	/**
	 * mark this is a command message
	 */
    CommandMessage,
    
    /**
     * mark this is a domain event stream message
     */
    DomainEventStreamMessage,

    /**
     * mark this is a exception message
     */
    ExceptionMessage,

    /**
     * mark this is a application message
     */
    ApplicationMessage,
    
}
