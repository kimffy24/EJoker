package pro.jiefzz.ejoker.common.utils.genericity;

import java.lang.reflect.Type;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pro.jk.ejoker.common.utils.genericity.GenericExpression;
import pro.jk.ejoker.common.utils.genericity.GenericExpressionFactory;
import pro.jk.ejoker.common.utils.genericity.TypeRefer;

public class TypeReferTest {

	@BeforeEach
	public void everyBefore() {
		System.out.println("==================== ");
	}

	@Test
	public void testARawType() {
		
		TypeRefer<String> sToken = new TypeRefer<String>(){};
		
		Type type = sToken.getType();
		
		Assertions.assertEquals(String.class, type);
		
	}

	/**
	 * 测试 通过 TypeRefer 得到Type  去构造GenericExpression 与 原始类型按泛型结构构造的GenericExpression事相同的。<br />
	 */
	@Test
	public void testBGenericType() {
		
		TypeRefer<List<String>> sToken = new TypeRefer<List<String>>(){};
		
		Type type = sToken.getType();
		
		GenericExpression genericExpress2 = GenericExpressionFactory.getGenericExpress(type);
		
		GenericExpression genericExpress = GenericExpressionFactory.getGenericExpress(List.class, String.class);
		
		Assertions.assertEquals(genericExpress, genericExpress2, "GenericExpression");
	}


	/**
	 * @see #testBGenericType()
	 */
	@Test
	public void testCGenericTypeDirectory() {
		
		GenericExpression genericExpress2 = GenericExpressionFactory.getGenericExpress((new TypeRefer<List<String>>(){}).getType());
		
		GenericExpression genericExpress = GenericExpressionFactory.getGenericExpress(List.class, String.class);
		
		Assertions.assertEquals(genericExpress, genericExpress2, "GenericExpression");
	}
	
}
