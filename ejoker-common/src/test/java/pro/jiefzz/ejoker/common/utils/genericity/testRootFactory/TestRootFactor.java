package pro.jiefzz.ejoker.common.utils.genericity.testRootFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import pro.jiefzz.ejoker.common.utils.genericity.element.DemoB;
import pro.jk.ejoker.common.utils.genericity.GenericExpression;
import pro.jk.ejoker.common.utils.genericity.GenericExpressionFactory;

public class TestRootFactor {

	@Test
	public void test() {
		
		GenericExpression middleStatementGenericExpression = GenericExpressionFactory.getMiddleStatementGenericExpression(DemoB.class);
		
		Assertions.assertNotNull(middleStatementGenericExpression);
		
	}
	
}
