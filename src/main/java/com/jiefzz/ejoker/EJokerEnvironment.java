package com.jiefzz.ejoker;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.utilities.Ensure;

/**
 * 在ejoker.properties中封装一些跟执行环境相关的变量
 * @author JiefzzLon
*/
public class EJokerEnvironment {
	
	public final static String ENVIROMMENT_FILE="ejoker.properties";

	//public final static String EXCHANGE_NAME;
	
	private final static Logger logger = LoggerFactory.getLogger(EJokerEnvironment.class);

	private final static Map<String, String> topicQueueMapper = new HashMap<String, String>();
	
	static {

		//## region start 加载相关公共变量配置
		Properties props = new Properties();
		try{
	 		props.load(EJokerEnvironment.class.getClassLoader().getResourceAsStream(ENVIROMMENT_FILE));
		}catch(Exception e){
			//throw new RuntimeException(EJokerEnvironment.class.getName()+" initialize faild!!!", e);
			logger.warn("Could not load configure information from {}!", ENVIROMMENT_FILE);
		}
		//## region end

		// 获取ejoker使用的交换机
		//EXCHANGE_NAME = props.getProperty("ejoker.rabbitmq.defaultExchange", "ejoker");
		
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
	static public String getTopicQueue(String topic) {
		String queue = topicQueueMapper.get(topic);
		Ensure.notNullOrEmpty(queue, "queue");
		return queue;
	}
}
