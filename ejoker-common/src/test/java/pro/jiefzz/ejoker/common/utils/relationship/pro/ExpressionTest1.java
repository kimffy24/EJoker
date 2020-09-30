package pro.jiefzz.ejoker.common.utils.relationship.pro;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import pro.jiefzz.ejoker.common.utils.relationship.pro.JSONCuRuProTest.Favior;
import pro.jk.ejoker.common.service.IJSONStringConverterPro;
import pro.jk.ejoker.common.service.impl.JSONStringConverterProUseJsonSmartImpl;
import pro.jk.ejoker.common.utils.genericity.TypeRefer;

public class ExpressionTest1 {

	private final static IJSONStringConverterPro pro = new JSONStringConverterProUseJsonSmartImpl();

	public final static class ClassmateBook2<T> {
		private T[] set4T = null;

		@Override
		public String toString() {
			return "ClassmateBook2 [set4T=" + Arrays.toString(set4T) + "]";
		}
		
	}
	
	public final static String tStr = "{\"set4T\":[[{\"name\":\"九寨沟\",\"address\":\"四川\"},{\"name\":\"雪乡\",\"address\":\"黑龙江\"}],[{\"name\":\"宝宝巴士\",\"address\":\"coco's iphone\"},{\"name\":\"邻居家的大龙猫\",\"address\":\"mac:8002\"}]]}";
	
	@Test
	public void test() {
		
		ClassmateBook2<Favior[]> revert = pro.revert(tStr, new TypeRefer<ClassmateBook2<Favior[]>>() {});
		System.err.println(revert);
	}
}
