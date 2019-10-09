package pro.jiefzz.ejoker.queue.skeleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.queue.skeleton.aware.IConsumerWrokerAware;
import pro.jiefzz.ejoker.queue.skeleton.aware.IEJokerQueueMessageContext;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.service.IScheduleService;
import pro.jiefzz.ejoker.z.service.IWorkerService;

public abstract class AbstractEJokerQueueConsumer implements IWorkerService {

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(AbstractEJokerQueueConsumer.class);

	/// #fix 180920 register sync offset task
	@Dependence
	protected IScheduleService scheduleService;
	
	private String syncOffsetTaskName = "";
	
	private static long taskIndex = 0;
	///

	private IConsumerWrokerAware consumer;

	public AbstractEJokerQueueConsumer useConsumer(IConsumerWrokerAware consumer) {
		this.consumer = consumer;
		return this;
	}
	
	public AbstractEJokerQueueConsumer start() {
		consumer.registerEJokerCallback(this::handle);
		try {
			consumer.start();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		/// #fix 180920 register sync offset task
		{
			scheduleService.startTask((syncOffsetTaskName = this.getClass().getSimpleName() + "#sync_offset_task#" + taskIndex++), consumer::loopInterval, getConsumerLoopInterval(), getConsumerLoopInterval());
		}
		///
		
		return this;
	}

	public AbstractEJokerQueueConsumer subscribe(String topic) throws Exception {
		consumer.subscribe(topic, "*");
		return this;
	}

	public AbstractEJokerQueueConsumer shutdown() {
		try {
			consumer.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}

		/// #fix 180920 register sync offset task
		{
			scheduleService.stopTask(syncOffsetTaskName);
		}
		///
		
		return this;
	}
	
	public IConsumerWrokerAware getDeeplyConsumer() {
		return consumer;
	}
	
	abstract public void handle(EJokerQueueMessage queueMessage, IEJokerQueueMessageContext context);
	
	abstract protected long getConsumerLoopInterval();

}
