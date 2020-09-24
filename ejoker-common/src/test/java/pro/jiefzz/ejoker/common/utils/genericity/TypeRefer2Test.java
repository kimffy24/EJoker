package pro.jiefzz.ejoker.common.utils.genericity;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pro.jiefzz.ejoker.common.utils.genericity.element.DemoA;
import pro.jiefzz.ejoker.common.utils.genericity.element.DemoB;
import pro.jk.ejoker.common.utils.genericity.GenericExpression;

/**
 * 此测试证明泛型变量无法隔一层传递，
 * 而pro.jk.ejoker.common.utils.genericity包下的工具类正是为弥补这功能的
 * @author kimffy
 *
 */
public class TypeRefer2Test {

	@BeforeEach
	public void everyBefore() {
		System.out.println("==================== ");
	}

	@Test
	public void testDGenericTypejj() {
		
		DemoA<Set<Integer>> d = new DemoA<Set<Integer>>();
		
		GenericExpression genericExpress2 = d.test1();
		
		Assertions.assertNull(genericExpress2, "genericExpress2");
		
	}

	@Test
	public void testDGenericTypejj1() {
		
		DemoA<Set<Integer>> d = new DemoA<Set<Integer>>() {};
		
		GenericExpression genericExpress2 = d.test1();
		
		Assertions.assertNull(genericExpress2, "genericExpress2");
		
	}

	@Test
	public void testDGenericTypejj2() {
		
		DemoB d = new DemoB();
		
		GenericExpression genericExpress2 = d.test1();
		
		Assertions.assertNull(genericExpress2, "genericExpress2");
		
	}
	
}
