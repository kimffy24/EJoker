package com.jiefzz.ejoker;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 在ejoker.properties中封装一些跟执行环境相关的变量
 * @author JiefzzLon
*/
public final class EJokerEnvironment {
	
	private final static Logger logger = LoggerFactory.getLogger(EJokerEnvironment.class);

	public final static int ASYNC_INTERNAL_EXECUTE_THREADPOOL_SIZE;
	
	public final static int ASYNC_IO_RETRY_THREADPOLL_SIZE;

	public final static boolean ASYNC_EJOKER_MESSAGE_SEND;
	
	public final static int ASYNC_MESSAGE_SENDER_THREADPOLL_SIZE;
	
	public final static boolean FLOW_CONTROL_ON_PROCESSING;
	
	public final static int MAX_AMOUNT_OF_ON_PROCESSING_MESSAGE;
	
	public final static long MAILBOX_IDLE_TIMEOUT;
	
	public final static long AGGREGATE_IN_MEMORY_EXPIRE_TIMEOUT;

	public final static int REPLY_PORT;

	/**
	 * 处理器数量
	 */
	public final static int NUMBER_OF_PROCESSOR = Runtime.getRuntime().availableProcessors();
	
	public final static boolean SUPPORT_BATCH_APPEND_EVENT;
	
	public final static int MAX_BATCH_COMMANDS;
	
	public final static int MAX_BATCH_EVENTS;
	
	public final static String ENVIROMMENT_FILE="ejoker.properties";

	static {

		// ## region start 加载相关公共变量配置
		Properties props = new Properties();
		try{
	 		props.load(EJokerEnvironment.class.getClassLoader().getResourceAsStream(ENVIROMMENT_FILE));
		}catch(Exception e){
			logger.warn("Could not load configure information from {}!", ENVIROMMENT_FILE);
			throw new RuntimeException(e);
		}
		// ## region end
		
		ASYNC_EJOKER_MESSAGE_SEND =
				Boolean.valueOf(props.getProperty("ASYNC_EJOKER_MESSAGE_SEND", "false"));

		ASYNC_INTERNAL_EXECUTE_THREADPOOL_SIZE =
				Integer.valueOf(props.getProperty("ASYNC_INTERNAL_EXECUTE_THREADPOOL_SIZE", "256"));

		ASYNC_IO_RETRY_THREADPOLL_SIZE =
				Integer.valueOf(props.getProperty("ASYNC_IO_RETRY_THREADPOLL_SIZE", "64"));
		
		ASYNC_MESSAGE_SENDER_THREADPOLL_SIZE =
				Integer.valueOf(props.getProperty("ASYNC_MESSAGE_SENDER_THREADPOLL_SIZE", "128"));

		FLOW_CONTROL_ON_PROCESSING =
				Boolean.valueOf(props.getProperty("FLOW_CONTROL_ON_PROCESSING", "false"));

		MAX_AMOUNT_OF_ON_PROCESSING_MESSAGE =
				Integer.valueOf(props.getProperty("MAX_AMOUNT_OF_ON_PROCESSING_MESSAGE", "" + (ASYNC_INTERNAL_EXECUTE_THREADPOOL_SIZE/2 + NUMBER_OF_PROCESSOR)));
		
		MAILBOX_IDLE_TIMEOUT =
				Long.valueOf(props.getProperty("MAILBOX_IDLE_TIMEOUT", "180000"));

		AGGREGATE_IN_MEMORY_EXPIRE_TIMEOUT =
				Long.valueOf(props.getProperty("AGGREGATE_IN_MEMORY_EXPIRE_TIMEOUT", "180000"));

		REPLY_PORT =
				Integer.valueOf(props.getProperty("REPLY_PORT", "65056"));
		
		SUPPORT_BATCH_APPEND_EVENT = 
				Boolean.valueOf(props.getProperty("SUPPORT_BATCH_APPEND_EVENT", "false"));

		MAX_BATCH_COMMANDS =
				Integer.valueOf(props.getProperty("MAX_BATCH_COMMANDS", "16"));

		MAX_BATCH_EVENTS =
				Integer.valueOf(props.getProperty("MAX_BATCH_EVENTS", "32"));

		logger.debug("ASYNC_INTERNAL_EXECUTE_THREADPOOL_SIZE: {}", ASYNC_INTERNAL_EXECUTE_THREADPOOL_SIZE);
		logger.debug("ASYNC_IO_RETRY_THREADPOLL_SIZE: {}", ASYNC_IO_RETRY_THREADPOLL_SIZE);
		logger.debug("ASYNC_EJOKER_MESSAGE_SEND: {}", ASYNC_EJOKER_MESSAGE_SEND);
		logger.debug("ASYNC_MESSAGE_SENDER_THREADPOLL_SIZE: {}", ASYNC_MESSAGE_SENDER_THREADPOLL_SIZE);
		logger.debug("MAX_AMOUNT_OF_ON_PROCESSING_MESSAGE: {}", MAX_AMOUNT_OF_ON_PROCESSING_MESSAGE);
		logger.debug("MAILBOX_IDLE_TIMEOUT: {}", MAILBOX_IDLE_TIMEOUT);
		logger.debug("AGGREGATE_IN_MEMORY_EXPIRE_TIMEOUT: {}", AGGREGATE_IN_MEMORY_EXPIRE_TIMEOUT);
		logger.debug("REPLY_PORT: {}", REPLY_PORT);
		logger.debug("SUPPORT_BATCH_APPEND_EVENT: {}", SUPPORT_BATCH_APPEND_EVENT);
		logger.debug("MAX_BATCH_COMMANDS: {}", MAX_BATCH_COMMANDS);
		logger.debug("MAX_BATCH_EVENTS: {}", MAX_BATCH_EVENTS);
	}

}
