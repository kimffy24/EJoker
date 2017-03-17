package com.jiefzz.ejoker.infrastructure.impl;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.infrastructure.IMessage;
import com.jiefzz.ejoker.infrastructure.IMessageDispatcher;
import com.jiefzz.ejoker.infrastructure.IProcessingMessage;
import com.jiefzz.ejoker.infrastructure.IProcessingMessageScheduler;
import com.jiefzz.ejoker.infrastructure.ProcessingMessageMailbox;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.task.AsyncPool;
import com.jiefzz.ejoker.z.common.task.IAsyncTask;
import com.jiefzz.ejoker.z.common.task.ThreadPoolMaster;

@EService
public class DefaultProcessingMessageScheduler<X extends IProcessingMessage<X, Y>, Y extends IMessage> implements IProcessingMessageScheduler<X, Y> {

	@Dependence
	IMessageDispatcher messageDispatcher;
	
	@Override
	public void scheduleMessage(final X processingMessage) {
//		(new Thread(new Runnable() {
//			@Override
//			public void run() {
//				messageDispatcher.dispatchMessageAsync(processingMessage.getMessage());
//			}
//		})).start();
		poolInstance.execute(new IAsyncTask<Boolean>(){
			public Boolean call() throws Exception {
				messageDispatcher.dispatchMessageAsync(processingMessage.getMessage());
				return null;
			}
		});
	}

	@Override
	public void scheduleMailbox(final ProcessingMessageMailbox<X, Y> mailbox) {
//		(new Thread(mailbox)).start();
		poolInstance.execute(new IAsyncTask<Boolean>(){
			public Boolean call() throws Exception {
				mailbox.run();
				return null;
			}
		});
	}


	// =================== thread strategy
    
    private final static AsyncPool poolInstance = ThreadPoolMaster.getPoolInstance(DefaultProcessingMessageScheduler.class, EJokerEnvironment.THREAD_POOL_SIZE);
	
}
