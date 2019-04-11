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

	/**
	 * EJoker内部任务线程池的大小<br>
	 * * 默认64
	 */
	public final static int ASYNC_INTERNAL_EXECUTE_THREADPOOL_SIZE;
	
	/**
	 * EJoker内部任务IO失败重试线程池的大小<br>
	 * * 默认8
	 */
	public final static int ASYNC_IO_RETRY_THREADPOLL_SIZE;

	/**
	 * 消息发送是否使用单独的线程池（独立发送者线程池）<br>
	 * * 默认false
	 * * 主要是针对quasar做的兼容，市面上大部分库都不是quasar-friendly型的io库
	 */
	public final static boolean ASYNC_EJOKER_MESSAGE_SEND;

	/**
	 * 独立发送者线程池的大小<br>
	 * * 默认128
	 */
	public final static int ASYNC_EJOKER_MESSAGE_SENDER_THREADPOLL_SIZE;
	
	/**
	 * 流控标识<br>
	 * * 默认false
	 */
	public final static boolean FLOW_CONTROL_ON_PROCESSING;
	
	/**
	 * 流控阈值<br>
	 * * 默认值为 ASYNC_INTERNAL_EXECUTE_THREADPOOL_SIZE/2 + NUMBER_OF_PROCESSOR
	 */
	public final static int MAX_AMOUNT_OF_ON_PROCESSING_MESSAGE;
	
	/**<br>
	 * mailbox超时清理阈值(单位: 毫秒)<br>
	 * * 默认值 180000 (即3分钟)
	 */
	public final static long MAILBOX_IDLE_TIMEOUT;

	/**<br>
	 * 内存聚合根的超时清理阈值(单位: 毫秒)<br>
	 * * 默认值 180000 (即3分钟)
	 */
	public final static long AGGREGATE_IN_MEMORY_EXPIRE_TIMEOUT;

	/**
	 * 消息队列的re-balance策略<br>
	 * 1. Average			满足均匀分布 ( * 默认值 )<br>
	 * 2. ConsistentHash	满足一致性哈希分布<br>
	 * * 其他策略暂不支持。
	 */
	public final static int REBALANCE_STRATEGY;

	/**
	 * 节点rpc侦听端口，用于接收命令或事件执行结果<br>
	 * * 默认 65056<br>
	 * * 请注意防火墙是否会阻止通讯
	 */
	public final static int REPLY_PORT;

	/**
	 * 处理器数量<br>
	 * * 默认从运行时中获取
	 */
	public final static int NUMBER_OF_PROCESSOR = Runtime.getRuntime().availableProcessors();
	
	/**
	 * commandMailbox单次run加载最大命令个数<br>
	 * * 默认32
	 */
	public final static int MAX_BATCH_COMMANDS;

	/**
	 * eventStreamMailbox单次run加载最大命令个数<br>
	 * * 默认32
	 */
	public final static int MAX_BATCH_EVENTS;
	
	/**
	 * 默认配置文件名
	 */
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

		ASYNC_INTERNAL_EXECUTE_THREADPOOL_SIZE =
				Integer.valueOf(props.getProperty("ASYNC_INTERNAL_EXECUTE_THREADPOOL_SIZE", "64"));

		ASYNC_IO_RETRY_THREADPOLL_SIZE =
				Integer.valueOf(props.getProperty("ASYNC_IO_RETRY_THREADPOLL_SIZE", "8"));
		
		ASYNC_EJOKER_MESSAGE_SEND =
				Boolean.valueOf(props.getProperty("ASYNC_EJOKER_MESSAGE_SEND", "false"));
		
		ASYNC_EJOKER_MESSAGE_SENDER_THREADPOLL_SIZE =
				Integer.valueOf(props.getProperty("ASYNC_EJOKER_MESSAGE_SENDER_THREADPOLL_SIZE", "128"));

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
		
		MAX_BATCH_COMMANDS =
				Integer.valueOf(props.getProperty("MAX_BATCH_COMMANDS", "1"));

		MAX_BATCH_EVENTS =
				Integer.valueOf(props.getProperty("MAX_BATCH_EVENTS", "32"));
		
		REBALANCE_STRATEGY = 
				Integer.valueOf(props.getProperty("REBALANCE_STRATEGY", "1"));

		logger.debug("ASYNC_INTERNAL_EXECUTE_THREADPOOL_SIZE: {}", ASYNC_INTERNAL_EXECUTE_THREADPOOL_SIZE);
		logger.debug("ASYNC_IO_RETRY_THREADPOLL_SIZE: {}", ASYNC_IO_RETRY_THREADPOLL_SIZE);
		logger.debug("ASYNC_EJOKER_MESSAGE_SEND: {}", ASYNC_EJOKER_MESSAGE_SEND);
		logger.debug("ASYNC_EJOKER_MESSAGE_SENDER_THREADPOLL_SIZE: {}", ASYNC_EJOKER_MESSAGE_SENDER_THREADPOLL_SIZE);
		logger.debug("MAX_AMOUNT_OF_ON_PROCESSING_MESSAGE: {}", MAX_AMOUNT_OF_ON_PROCESSING_MESSAGE);
		logger.debug("MAILBOX_IDLE_TIMEOUT: {}", MAILBOX_IDLE_TIMEOUT);
		logger.debug("AGGREGATE_IN_MEMORY_EXPIRE_TIMEOUT: {}", AGGREGATE_IN_MEMORY_EXPIRE_TIMEOUT);
		logger.debug("REPLY_PORT: {}", REPLY_PORT);
		logger.debug("MAX_BATCH_COMMANDS: {}", MAX_BATCH_COMMANDS);
		logger.debug("MAX_BATCH_EVENTS: {}", MAX_BATCH_EVENTS);
		logger.debug("REBALANCE_STRATEGY: {}", REBALANCE_STRATEGY);
	}

}
