package com.jiefzz.ejoker.z.queue.clients.producers;

import java.io.IOException;

public interface IMessageProducer {

	public void produce(String key, String msg) throws IOException;
	public void produce(String key, byte[] msg) throws IOException;
	public void onProducerThreadClose();

}
