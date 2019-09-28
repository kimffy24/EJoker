package pro.jiefzz.ejoker.infrastructure.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pro.jiefzz.ejoker.infrastructure.IPublishedVersionStore;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapperUtil;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

/**
 * 已经发布事件版本记录中心服务。
 * @author kimffy
 *
 */
//@EService
public class InMemoryPublishedVersionStore implements IPublishedVersionStore {

	private final SystemFutureWrapper<AsyncTaskResult<Void>> successTask
		= SystemFutureWrapperUtil.completeFutureTask();

	private final Map<String, Long> versionDict = new ConcurrentHashMap<>();

	@Override
	public SystemFutureWrapper<AsyncTaskResult<Void>> updatePublishedVersionAsync(String processorName, String aggregateRootTypeName,
			String aggregateRootId, long publishedVersion) {
		updatePublishedVersion(processorName, aggregateRootTypeName, aggregateRootId, publishedVersion);
		return successTask;
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<Long>> getPublishedVersionAsync(String processorName, String aggregateRootTypeName,
			String aggregateRootId) {
		return SystemFutureWrapperUtil.completeFutureTask(getPublishedVersion(processorName, aggregateRootTypeName, aggregateRootId));
		
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
