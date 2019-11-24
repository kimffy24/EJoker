package pro.jiefzz.ejoker.queue.skeleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.common.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.common.service.IScheduleService;
import pro.jiefzz.ejoker.common.service.IWorkerService;
import pro.jiefzz.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.queue.skeleton.aware.IConsumerWrokerAware;
import pro.jiefzz.ejoker.queue.skeleton.aware.IEJokerQueueMessageContext;

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

	public AbstractEJokerQueueConsumer subscribe(String topic) throws Exception {
		consumer.subscribe(topic, "*");
		return this;
	}
	
	public IConsumerWrokerAware getDeeplyConsumer() {
		return consumer;
	}

	@Override
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

	@Override
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
	
	@Override
	public boolean isAllReady() {
		return this.consumer.isAllReady();
	}

	abstract public void handle(EJokerQueueMessage queueMessage, IEJokerQueueMessageContext context);
	
	/**
	 * 我们定义一个脉冲线程，定时执行一次内部的consumer的loopInterval方法<br />
	 * 通过这个抽象方法提供给子类自定义脉冲时间间隔<br />
	 * @return 脉冲间隔(单位: ms)
	 */
	abstract protected long getConsumerLoopInterval();

}
