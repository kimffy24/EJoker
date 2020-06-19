package pro.jiefzz.ejoker.common.utils.genericity;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import pro.jiefzz.ejoker.common.utils.genericity.element.DemoA;
import pro.jk.ejoker.common.utils.genericity.GenericExpression;
import pro.jk.ejoker.common.utils.genericity.GenericExpressionFactory;
import pro.jk.ejoker.common.utils.genericity.TypeRefer;

public class TypeReferTest {

	@Test
	public void testARawType() {
		
		TypeRefer<String> sToken = new TypeRefer<String>(){};
		
		Type type = sToken.getType();
		
		Assertions.assertEquals(String.class, type);
		
	}

	@Test
	public void testBGenericType() {
		
		TypeRefer<List<String>> sToken = new TypeRefer<List<String>>(){};
		
		Type type = sToken.getType();
		
		GenericExpression genericExpress2 = GenericExpressionFactory.getGenericExpress(type);
		
		GenericExpression genericExpress = GenericExpressionFactory.getGenericExpress(List.class, String.class);
		
		Assertions.assertEquals(genericExpress, genericExpress2, "GenericExpression");
	}


	@Test
	public void testCGenericTypeDirectory() {
		
		GenericExpression genericExpress2 = GenericExpressionFactory.getGenericExpress((new TypeRefer<List<String>>(){}).getType());
		
		GenericExpression genericExpress = GenericExpressionFactory.getGenericExpress(List.class, String.class);
		
		Assertions.assertEquals(genericExpress, genericExpress2, "GenericExpression");
	}
	
}
