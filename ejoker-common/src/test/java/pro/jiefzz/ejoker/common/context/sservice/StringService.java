package pro.jiefzz.ejoker.common.context.sservice;

import pro.jk.ejoker.common.context.annotation.context.EService;

@EService
public class StringService implements ISuperA<String> {

	@Override
	public String add(String t1, String t2) {
		return "" + t1 + t2;
	}

}
