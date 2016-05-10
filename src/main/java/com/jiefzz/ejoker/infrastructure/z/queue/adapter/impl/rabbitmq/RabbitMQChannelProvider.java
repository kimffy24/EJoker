package com.jiefzz.ejoker.infrastructure.z.queue.adapter.impl.rabbitmq;

import java.io.IOException;
import java.util.Properties;

import com.jiefzz.ejoker.infrastructure.InfrastructureRuntimeException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQChannelProvider {

	private final static String configFileName = "rabbitmq.properties";
	private final static ConnectionFactory factory;
	private final Connection connection;
	
	public final static String EXCHANGE_NAME;

	public RabbitMQChannelProvider() {
		try {
			connection = factory.newConnection();
		} catch ( Exception e ) {
			throw new InfrastructureRuntimeException("Could not connect to rabbitmq server!!!", e);
		}
	}

	public Channel getNewChannel() {
		try {
			return connection.createChannel();
		} catch (IOException e) {
			throw new InfrastructureRuntimeException("Could not create a new rabbitmq channel!!!", e);
		}
	}

	static{
		// While the ClassLoader load this class,
		// build rabbitmq connection factory Object.
		Properties props = new Properties();
		try {
			props.load(RabbitMQChannelProvider.class.getClassLoader().getResourceAsStream(configFileName));
		}catch (IOException e) {
			throw new InfrastructureRuntimeException("Can not load "+configFileName, e);
		}
		factory = new ConnectionFactory();
		factory.setHost(props.getProperty("rabbitmq.host", "localhost"));
		factory.setPort(Integer.parseInt(props.getProperty("rabbitmq.port", "localhost")));
		factory.setUsername(props.getProperty("rabbitmq.username", "guest"));
		factory.setPassword(props.getProperty("rabbitmq.password", "guest"));
		
		EXCHANGE_NAME = props.getProperty("ejoker.rabbitmq.defaultExchange", "ejoker");
	}
}
