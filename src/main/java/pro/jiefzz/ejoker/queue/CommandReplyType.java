package pro.jiefzz.ejoker.queue;

public enum CommandReplyType {
	/**
	 * unuse 0
	 */
	Undefined,

	/**
	 * it means eventStroe save the event data
	 */
	CommandExecuted,

	/**
	 * it means eventComsumer handled event data
	 */
	DomainEventHandled

}
