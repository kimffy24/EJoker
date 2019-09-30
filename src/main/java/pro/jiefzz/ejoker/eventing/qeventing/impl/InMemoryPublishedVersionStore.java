package pro.jiefzz.ejoker.eventing.qeventing.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import pro.jiefzz.ejoker.eventing.qeventing.IPublishedVersionStore;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.EJokerFutureTaskUtil;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.EJokerFutureUtil;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

/**
 * 已经发布事件版本记录中心服务。
 * @author kimffy
 *
 */
//@EService
public class InMemoryPublishedVersionStore implements IPublishedVersionStore {

	private final Map<String, Long> versionDict = new ConcurrentHashMap<>();

	@Override
	public Future<AsyncTaskResult<Void>> updatePublishedVersionAsync(String processorName, String aggregateRootTypeName,
			String aggregateRootId, long publishedVersion) {
		updatePublishedVersion(processorName, aggregateRootTypeName, aggregateRootId, publishedVersion);
		return EJokerFutureTaskUtil.completeTask();
	}

	@Override
	public Future<AsyncTaskResult<Long>> getPublishedVersionAsync(String processorName, String aggregateRootTypeName,
			String aggregateRootId) {
		return EJokerFutureTaskUtil.completeTask(getPublishedVersion(processorName, aggregateRootTypeName, aggregateRootId));
		
	}

	private void updatePublishedVersion(String processorName, String aggregateRootTypeName, String aggregateRootId,
			long publishedVersion) {
		versionDict.put(buildKey(processorName, aggregateRootId), publishedVersion);
	}

	private long getPublishedVersion(String processorName, String aggregateRootTypeName, String aggregateRootId) {
		return versionDict.getOrDefault(buildKey(processorName, aggregateRootId), 0l);
	}

	private String buildKey(String eventProcessorName, String aggregateRootId) {
		return String.format("%s-%s", eventProcessorName, aggregateRootId);
	}
	
}
