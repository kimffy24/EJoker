package pro.jk.ejoker.eventing.qeventing;

import java.util.concurrent.Future;

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
	Future<Void> updatePublishedVersionAsync(String processorName, String aggregateRootTypeName, String aggregateRootId, long publishedVersion);
    
    /**
     * Get the current published version for the given aggregate.
     * @param processorName
     * @param aggregateRootTypeName
     * @param aggregateRootId
     * @return
     */
	Future<Long> getPublishedVersionAsync(String processorName, String aggregateRootTypeName, String aggregateRootId);

}
