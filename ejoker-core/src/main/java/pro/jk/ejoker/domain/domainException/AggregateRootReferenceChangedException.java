package pro.jk.ejoker.domain.domainException;

import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.domain.IAggregateRoot;

public class AggregateRootReferenceChangedException extends RuntimeException {

	private static final long serialVersionUID = 7817158077437651384L;
	
	private final static String MsgTpl = "Aggregate root reference already changed!!! [id={}, type={}]";

	public final IAggregateRoot AggregateRoot;
	
	public AggregateRootReferenceChangedException(IAggregateRoot aggr) {
		super(StringUtilx.fmt(MsgTpl, aggr.getUniqueId(), aggr.getClass().getName()));
		this.AggregateRoot = aggr;
	}
	
}
