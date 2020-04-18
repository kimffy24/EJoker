package pro.jiefzz.ejoker.test.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pro.jiefzz.ejoker.test.ins.ClassA;
import pro.jiefzz.ejoker.test.ins.ClassB;
import pro.jiefzz.ejoker.test.ins.ClassC;
import pro.jiefzz.ejoker.test.ins.ClassD;
import pro.jiefzz.ejoker.test.ins.ClassE;
import pro.jiefzz.ejoker.test.ins.ClassF;
import pro.jk.ejoker.common.utils.GenericTypeUtil;

class T5GenericTypeUtil {

	@BeforeEach
	public void everyBefore() {
		System.out.println("==================== ");
	}
	
	@Disabled
	@DisplayName("GenericTypeUtil.emptyParametersBook")
	public void testStaticProperties() {
		
		GenericTypeUtil.emptyParametersBook.entrySet().forEach(e -> {
			System.out.println(String.format("%2d => %s", e.getKey(), e.getValue()));
		});
		
	}
	
	@Test
	@DisplayName("GenericTypeUtil.getDeclaredGenericSignature")
	void testGetDeclaredGenericSignature() {
		
		Field declaredField1;
		try {
			declaredField1 = ClassA.class.getDeclaredField("y");
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
			fail(e.getMessage());
			return;
		}

		Field declaredField2;
		try {
			declaredField2 = ClassA.class.getDeclaredField("z");
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
			fail(e.getMessage());
			return;
		}
		
		Field declaredField3;
		try {
			declaredField3 = ClassA.class.getDeclaredField("l");
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
			fail(e.getMessage());
			return;
		}
		
		System.out.println(declaredField1.getGenericType().getTypeName());
		assertEquals("", GenericTypeUtil.getDeclaredGenericSignature(declaredField1), "ClassA#y faild!!!");

		System.out.println(declaredField2.getGenericType().getTypeName());
		assertEquals(String.format("<%s, %s>", String.class.getName(), String.class.getName()),
				GenericTypeUtil.getDeclaredGenericSignature(declaredField2),
				"ClassA#z faild!!!");

		System.out.println(declaredField3.getGenericType().getTypeName());
		assertNotEquals(String.format("<%s>", Object.class.getName()),
				GenericTypeUtil.getDeclaredGenericSignature(declaredField3),
				"ClassA#l faild!!!");
		
	}

	@Test
	@DisplayName("GenericTypeUtil.getClassDefinationGenericSignature(Class<?>)")
	void testGetClassDefinationGenericSignatureClassOfQ() {
		assertEquals(String.format("<%s>", "T"),
				GenericTypeUtil.getClassDefinationGenericSignature(ClassA.class),
				"ClassA"
				);
		assertEquals("",
				GenericTypeUtil.getClassDefinationGenericSignature(ClassB.class),
				"ClassB");
		assertEquals("",
				GenericTypeUtil.getClassDefinationGenericSignature(ClassC.class),
				"ClassC");
		assertEquals(String.format("<%s>", "U"),
				GenericTypeUtil.getClassDefinationGenericSignature(ClassD.class),
				"ClassD"
				);
		
	}

	@Test
	@DisplayName("GenericTypeUtil.getClassDefinationGenericSignature(Type)")
	void testGetClassDefinationGenericSignatureType() {
		// 无法测试，仅对逻辑结构进行演示
		for(Class<?> clazz : new Class<?>[] {ClassE.class, ClassF.class})  {
			
			System.out.println(String.format("%s: %s",
					clazz.getSimpleName(),
					GenericTypeUtil.getClassDefinationGenericSignature(clazz)
				));
			
			Type genericSuperclass = clazz.getGenericSuperclass();
			System.out.println(String.format(" S -> %s: %s",
					genericSuperclass.getTypeName().replaceAll("<([\\s\\S]*?)$", ""),
					GenericTypeUtil.getClassDefinationGenericSignature(genericSuperclass)
				));
			
			Type[] genericInterfaces = clazz.getGenericInterfaces();
			for(Type genericInterface : genericInterfaces) {
				System.out.println(String.format(" I -> %s: %s",
						genericInterface.getTypeName().replaceAll("<([\\s\\S]*?)$", ""),
						GenericTypeUtil.getClassDefinationGenericSignature(genericInterface)
					));
			}
			
		}
	}

}
