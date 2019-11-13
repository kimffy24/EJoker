package pro.jiefzz.ejoker.commanding;

import java.util.Map;

import pro.jiefzz.ejoker.messaging.AbstractMessage;
import pro.jiefzz.ejoker.z.system.helper.Ensure;

/**
 * ENode 中Command基类分了不带泛型的和带泛型的Command<TAggregateRootId>
 * 这里统一为泛型。
 * @author kimffy
 *
 */
public class AbstractCommand<TAggregateRootId> extends AbstractMessage implements ICommand {
	
	private TAggregateRootId aggregateRootId;

	public AbstractCommand(){
		super();
	}
	
	public AbstractCommand(TAggregateRootId aggregateRootId){
		this(aggregateRootId, null);
	}
	
	public AbstractCommand(TAggregateRootId aggregateRootId, Map<String, String> items) {
		Ensure.notNull(aggregateRootId, "aggregateRootId");
		this.aggregateRootId = aggregateRootId;
		this.setItems(items);
	}

	public void setAggregateRootId(TAggregateRootId aggregateRootId) {
		Ensure.notNull(aggregateRootId, "aggregateRootId");
		this.aggregateRootId = aggregateRootId;
	}

	// 得益于MSIL优秀的机制，C#多态比java牛逼多了
	public TAggregateRootId getAggregateRootIdRow() {
//	public TAggregateRootId getAggregateRootId() {
		return this.aggregateRootId;
	}

	@Override
	public String getAggregateRootId() {
		return null == aggregateRootId ? null : aggregateRootId.toString();
	}

}
