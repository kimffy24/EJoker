package pro.jk.ejoker.eventing.qeventing.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.system.extension.acrossSupport.EJokerFutureUtil;
import pro.jk.ejoker.eventing.qeventing.IPublishedVersionStore;

/**
 * 已经发布事件版本记录中心服务。
 * @author kimffy
 *
 */
//@EService
public class InMemoryPublishedVersionStore implements IPublishedVersionStore {

	private final Map<String, Long> versionDict = new ConcurrentHashMap<>();

	@Override
	public Future<Void> updatePublishedVersionAsync(String processorName, String aggregateRootTypeName,
			String aggregateRootId, long publishedVersion) {
		updatePublishedVersion(processorName, aggregateRootTypeName, aggregateRootId, publishedVersion);
		return EJokerFutureUtil.completeFuture();
	}

	@Override
	public Future<Long> getPublishedVersionAsync(String processorName, String aggregateRootTypeName,
			String aggregateRootId) {
		return EJokerFutureUtil.completeFuture(getPublishedVersion(processorName, aggregateRootTypeName, aggregateRootId));
	}

	private void updatePublishedVersion(String processorName, String aggregateRootTypeName, String aggregateRootId,
			long publishedVersion) {
		versionDict.put(buildKey(processorName, aggregateRootId), publishedVersion);
	}

	private long getPublishedVersion(String processorName, String aggregateRootTypeName, String aggregateRootId) {
		return versionDict.getOrDefault(buildKey(processorName, aggregateRootId), 0l);
	}

	private String buildKey(String eventProcessorName, String aggregateRootId) {
		return StringUtilx.fmt("{}-{}", eventProcessorName, aggregateRootId);
	}
	
}
