package pro.jiefzz.ejoker.test.z.utils.genericity;

import org.junit.jupiter.api.Test;

import pro.jiefzz.ejoker.common.utils.genericity.GenericExpression;
import pro.jiefzz.ejoker.common.utils.genericity.GenericExpressionFactory;
import pro.jiefzz.ejoker.test.ins.ClassF;

class T5 {

	@Test
	void test() {
		GenericExpression genericExpress = GenericExpressionFactory.getGenericExpress(ClassF.class);
	}

}
