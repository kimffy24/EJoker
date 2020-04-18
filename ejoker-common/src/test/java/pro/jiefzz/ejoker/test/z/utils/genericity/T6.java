package pro.jiefzz.ejoker.test.z.utils.genericity;

import org.junit.jupiter.api.Test;

import pro.jiefzz.ejoker.test.ins.ClassA;
import pro.jk.ejoker.common.utils.genericity.GenericExpression;

class T6 {

	@Test
	void test() {

		System.out.println(GenericExpression.getExpressionSignature(ClassA.class));
		
		System.out.println(GenericExpression.getExpressionSignature(ClassA.class));
		
	}

}
