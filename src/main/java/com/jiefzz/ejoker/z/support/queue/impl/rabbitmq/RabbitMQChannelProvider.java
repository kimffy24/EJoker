package com.jiefzz.ejoker.z.support.queue.impl.rabbitmq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Set;

import com.jiefzz.ejoker.infrastructure.InfrastructureRuntimeException;
import com.jiefzz.ejoker.z.common.scavenger.Scavenger;
import com.jiefzz.ejoker.z.common.utilities.Ensure;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQChannelProvider {
	
	private final static  Logger logger = LoggerFactory.getLogger(RabbitMQChannelProvider.class);

	private final static String configFileName = "rabbitmq.properties";
	private final static Properties props = new Properties();
	private final static ConnectionFactory factory;
	private final static Connection connection;
	
	public final static String EXCHANGE_NAME;

	private RabbitMQChannelProvider() { }

	public Channel getNewChannel() {
		try {
			return connection.createChannel();
		} catch (IOException e) {
			throw new InfrastructureRuntimeException("Could not create a new rabbitmq channel!!!", e);
		}
	}

	static{
		boolean configureError = false;
		// While the ClassLoader load this class,
		// build rabbitmq connection factory Object.
		try {
			props.load(RabbitMQChannelProvider.class.getClassLoader().getResourceAsStream(configFileName));
		}catch (Exception e) {
			logger.error("Can not load {}", configFileName);
			configureError = true;
			//throw new InfrastructureRuntimeException("Can not load "+configFileName, e);
		}
		factory = new ConnectionFactory();
		if(!configureError) {
			factory.setHost(props.getProperty("rabbitmq.host", "localhost"));
			factory.setPort(Integer.parseInt(props.getProperty("rabbitmq.port", "5672")));
			factory.setUsername(props.getProperty("rabbitmq.username", "guest"));
			factory.setPassword(props.getProperty("rabbitmq.password", "guest"));
			
			// 获取ejoker-rabbitmq使用的交换机
			EXCHANGE_NAME = props.getProperty("ejoker.rabbitmq.defaultExchange", "ejoker");
		} else {
			EXCHANGE_NAME = "ejoker";
		}

		try {
			connection = factory.newConnection();
			
			Scavenger.addFianllyJob(new Runnable() {
				@Override
				public void run() {
					try {
						RabbitMQChannelProvider.this.connection.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			
		} catch ( Exception e ) {
			throw new InfrastructureRuntimeException("Could not connect to rabbitmq server!!!", e);
		}
		
	}
	
	private static RabbitMQChannelProvider instance=null;
	
	/**
	 * 获取消息服务客户端实例
	 * 
	 * @return RabbitMQChannelProvider
	 */
	static public RabbitMQChannelProvider getInstance(){
		return (null!=instance)?instance:(instance = new RabbitMQChannelProvider());
	}

}
