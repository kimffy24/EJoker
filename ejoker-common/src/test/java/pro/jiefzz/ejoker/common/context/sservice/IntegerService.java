package pro.jiefzz.ejoker.common.context.sservice;

import pro.jk.ejoker.common.context.annotation.context.EService;

@EService
public class IntegerService implements ISuperA<Integer> {

	@Override
	public Integer add(Integer t1, Integer t2) {
		return t1 + t2;
	}

}
