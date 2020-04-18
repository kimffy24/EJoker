package pro.jiefzz.ejoker.test.z.utils.genericity;

import org.junit.jupiter.api.Test;

import pro.jiefzz.ejoker.test.ins.ClassF;
import pro.jk.ejoker.common.utils.genericity.GenericExpression;
import pro.jk.ejoker.common.utils.genericity.GenericExpressionFactory;

class T5 {

	@Test
	void test() {
		GenericExpression genericExpress = GenericExpressionFactory.getGenericExpress(ClassF.class);
	}

}
