package pro.jiefzz.ejoker.eventing.qeventing;

import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

/**
 * Represents a storage to store the aggregate published event version.
 * @author kimffy
 *
 */
public interface IPublishedVersionStore {
	
	/**
	 * Update the published version for the given aggregate.
	 * @param processorName
	 * @param aggregateRootTypeName
	 * @param aggregateRootId
	 * @param publishedVersion
	 * @return
	 */
	SystemFutureWrapper<AsyncTaskResult<Void>> updatePublishedVersionAsync(String processorName, String aggregateRootTypeName, String aggregateRootId, long publishedVersion);
    
    /**
     * Get the current published version for the given aggregate.
     * @param processorName
     * @param aggregateRootTypeName
     * @param aggregateRootId
     * @return
     */
	SystemFutureWrapper<AsyncTaskResult<Long>> getPublishedVersionAsync(String processorName, String aggregateRootTypeName, String aggregateRootId);

}
