package pro.jiefzz.ejoker.common.context.conflictService;

import java.math.BigInteger;

import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EService;

@EService
public class ServiceTop1 {

	@Dependence
	private IBSuper<BigInteger> serviceB_BI;
	
}
