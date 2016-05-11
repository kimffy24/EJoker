package com.jiefzz.ejoker.z.queue.adapter;

import java.io.IOException;

public interface IMessageQueue {

	public void produce(String key, String msg) throws IOException;
	public void produce(String key, byte[] msg) throws IOException;
	public void onProducerThreadClose();

}
