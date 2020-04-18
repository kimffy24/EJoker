package pro.jk.ejoker_support.javaqueue;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import pro.jk.ejoker.common.system.task.io.IOExceptionOnRuntime;
import pro.jk.ejoker.queue.skeleton.aware.EJokerQueueMessage;

public interface ICQProvider {

	Map<String, DSH> mockMsgQueues = new ConcurrentHashMap<>();;
	
	public final static class DSH {
		
		public final Queue<EJokerQueueMessage> queue = new ConcurrentLinkedQueue<>();
		
		public final AtomicInteger ai = new AtomicInteger(0);
		
		public void offer(EJokerQueueMessage msg) {
			if(queue.offer(msg))
				ai.getAndIncrement();
			else
				throw IOExceptionOnRuntime.encapsulation(new RuntimeException(""));
		}
	}
	
}
