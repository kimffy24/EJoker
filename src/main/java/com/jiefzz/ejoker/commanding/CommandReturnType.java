package com.jiefzz.ejoker.commanding;

/**
 * * Please do not change its order!
 * @author Administrator
 *
 */
public enum CommandReturnType {
	
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
	EventHandled
}