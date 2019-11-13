package pro.jiefzz.ejoker.eventing.qeventing;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.common.system.task.AsyncTaskResult;

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
	Future<AsyncTaskResult<Void>> updatePublishedVersionAsync(String processorName, String aggregateRootTypeName, String aggregateRootId, long publishedVersion);
    
    /**
     * Get the current published version for the given aggregate.
     * @param processorName
     * @param aggregateRootTypeName
     * @param aggregateRootId
     * @return
     */
	Future<AsyncTaskResult<Long>> getPublishedVersionAsync(String processorName, String aggregateRootTypeName, String aggregateRootId);

}
