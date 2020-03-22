package pro.jiefzz.ejoker.commanding;

import pro.jiefzz.ejoker.common.system.enhance.StringUtilx;

public class AggregateRootAlreadyExistException extends RuntimeException {

	private static final long serialVersionUID = -3883348844724461341L;
	
	private final static String exceptionMessageFillTpl = "Aggregate root already exist in command context, cannot be added again. [aggregateRootId: {}, aggregateRootType: {}]";

	public AggregateRootAlreadyExistException(Object id, @SuppressWarnings("rawtypes") Class type) {
		super(StringUtilx.fmt(exceptionMessageFillTpl, id, type.getName()));
	}

}
