package pro.jiefzz.ejoker_support.javaqueue;

import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import pro.jiefzz.ejoker.common.system.enhance.MapUtil;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction2;
import pro.jiefzz.ejoker.common.system.wrapper.DiscardWrapper;
import pro.jiefzz.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.queue.skeleton.aware.IConsumerWrokerAware;
import pro.jiefzz.ejoker.queue.skeleton.aware.IEJokerQueueMessageContext;

public class MQConsumerMemoryAdapter implements ICQProvider, IConsumerWrokerAware {
	
	private DSH dsh = null;

	private Queue<EJokerQueueMessage> queue = null;
	
	private IVoidFunction2<EJokerQueueMessage, IEJokerQueueMessageContext> vfHandler = null;
	
	private AtomicBoolean shutdownFlag = new AtomicBoolean(false);
	
	private AtomicBoolean readyFlag = new AtomicBoolean(false);
	
	private AtomicLong finishedAmount = new AtomicLong(0);
	
	private AtomicInteger sentAmount = null;
	
	@Override
	public void start() throws Exception {
		new Thread(() -> {
			if(null == queue)
				throw new RuntimeException("queue is null!!!");
			int i = 0;
			for( ;; ) {
				EJokerQueueMessage msg = queue.poll();
				if(null != msg) {
					i = 0;
					vfHandler.trigger(msg, m -> {
						finishedAmount.getAndIncrement();
					});
				} else {
					if(shutdownFlag.get())
						return;
					if(32 < ++ i ) {
						DiscardWrapper.sleepInterruptable(TimeUnit.MILLISECONDS, 1l);
						i = 0;
						readyFlag.compareAndSet(false, true);
					}
				}
			}
		}).start();
	}

	@Override
	public void shutdown() throws Exception {
		while(finishedAmount.get() < sentAmount.get() )
			DiscardWrapper.sleepInterruptable(TimeUnit.MILLISECONDS, 10l);
		shutdownFlag.set(true);
	}

	@Override
	public void subscribe(String topic, String filter) {
		queue = (dsh = MapUtil.getOrAdd(mockMsgQueues, topic, () -> new DSH())).queue;
		sentAmount = dsh.ai;
	}

	@Override
	public void registerEJokerCallback(IVoidFunction2<EJokerQueueMessage, IEJokerQueueMessageContext> vf) {
		vfHandler = vf;
	}

	@Override
	public void loopInterval() {
		
	}

	@Override
	public boolean isAllReady() {
		return readyFlag.get();
	}

}
