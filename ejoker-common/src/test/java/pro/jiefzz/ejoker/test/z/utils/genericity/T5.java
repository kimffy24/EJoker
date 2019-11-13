package pro.jiefzz.ejoker.test.z.utils.genericity;

import org.junit.jupiter.api.Test;

import pro.jiefzz.ejoker.test.z.utils.demo.ClassF;
import pro.jiefzz.ejoker.z.utils.genericity.GenericExpression;
import pro.jiefzz.ejoker.z.utils.genericity.GenericExpressionFactory;

class T5 {

	@Test
	void test() {
		GenericExpression genericExpress = GenericExpressionFactory.getGenericExpress(ClassF.class);
	}

}
