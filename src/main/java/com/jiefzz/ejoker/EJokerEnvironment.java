package com.jiefzz.ejoker;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.utils.Ensure;

/**
 * 在ejoker.properties中封装一些跟执行环境相关的变量
 * @author JiefzzLon
*/
public final class EJokerEnvironment {
	
	public final static String ENVIROMMENT_FILE="ejoker.properties";
	
	public final static long MAILBOX_IDLE_TIMEOUT = 180000l;

	public final static int REPLY_PORT = 65056;

	public final static int THREAD_POOL_SIZE = 4;
	
	public final static int MAX_BATCH_COMMANDS = 32;
	
	private final static Logger logger = LoggerFactory.getLogger(EJokerEnvironment.class);

	private final static Map<String, String> topicQueueMapper = new HashMap<String, String>();
	
	static {

		// ## region start 加载相关公共变量配置
		Properties props = new Properties();
		try{
	 		props.load(EJokerEnvironment.class.getClassLoader().getResourceAsStream(ENVIROMMENT_FILE));
		}catch(Exception e){
			logger.warn("Could not load configure information from {}!", ENVIROMMENT_FILE);
		}
		// ## region end

		// 提取写在配置文件中的主题队列配对
		Set<Entry<Object, Object>> entrySet = props.entrySet();
		for(Entry<Object, Object> entry : entrySet){
			String key = (String ) entry.getKey();
			if(key.startsWith("ejoker.topic.queue")){
				String topic = key.substring(1+"ejoker.topic.queue".length());
				String queue = (String ) entry.getValue();
				topicQueueMapper.put(topic, queue);
			}
		}

	}

	/**
	 * 查询主题对应的队列名，以便客户端消费
	 * @param topic
	 * @return
	 */
	static public String getQueueWhichFocusedTopic(String topic) {
		String queue = topicQueueMapper.get(topic);
		Ensure.notNullOrEmpty(queue, "queue");
		return queue;
	}
}
