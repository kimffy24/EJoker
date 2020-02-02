package pro.jiefzz.ejoker.common.system.helper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringHelperTest {

	@Test
	public void formatTest() {
		{
			String r1_tpl = "Hello kimffy, welcome to the world.";
			String r1_1 = "jiefzz";
			String r1Expected = "Hello kimffy, welcome to the world.";
			String r1Result = StringHelper.fill(r1_tpl, r1_1);
			
			Assertions.assertEquals(r1Expected, r1Result, "formatTest.0");
		}
		
		{
			String r1_tpl = "Hello {}, welcome to the world.";
			String r1_1 = "jiefzz";
			String r1Expected = "Hello jiefzz, welcome to the world.";
			String r1Result = StringHelper.fill(r1_tpl, r1_1);
			
			Assertions.assertEquals(r1Expected, r1Result, "formatTest.1");
		}

		{
			String r1_tpl = "Hello {}, welcome to the {} world.";
			String r1_1 = "jiefzz";
			String r1_2 = "java";
			String r1Expected = "Hello jiefzz, welcome to the java world.";
			String r1Result = StringHelper.fill(r1_tpl, r1_1, r1_2);
			
			Assertions.assertEquals(r1Expected, r1Result, "formatTest.2");
		}

		{
			String r1_tpl = "Hello {}, welcome to the {} world. now is {}";
			String r1_1 = "jiefzz";
			String r1_2 = "java";
			long r1_3 = 512l;
			String r1Expected = "Hello jiefzz, welcome to the java world. now is 512";
			String r1Result = StringHelper.fill(r1_tpl, r1_1, r1_2, r1_3);

			Assertions.assertEquals(r1Expected, r1Result, "formatTest.3");
		}

		{
			String r1_tpl = "{}: Hello {}, welcome to the {} world. now is {}";
			String r1Expected = "TEST: Hello jiefzz, welcome to the java world. now is 512";
			String r1Result = StringHelper.fill(r1_tpl, "TEST", "jiefzz", "java", 512);

			Assertions.assertEquals(r1Expected, r1Result, "formatTest.4");
		}

		{
			String r1_tpl = "{}: Hello {}, welcome to the {} world. now is {}, next is {}, latest is {}";
			String r1Expected = "TEST: Hello jiefzz, welcome to the java world. now is 512, next is {}, latest is {}";
			String r1Result = StringHelper.fill(r1_tpl, "TEST", "jiefzz", "java", 512);

			Assertions.assertEquals(r1Expected, r1Result, "formatTest.5");
		}

		{
			String r1_tpl = "{}: Hello {}, welcome to the {} world. now is {}, next is {}, latest is {}";
			String r1Expected = "TEST: Hello jiefzz, welcome to the java world. now is 512, next is 4096, latest is 16384";
			String r1Result = StringHelper.fill(r1_tpl, "TEST", "jiefzz", "java", 512, 4096, 16384, "你好", "世界");

			Assertions.assertEquals(r1Expected, r1Result, "formatTest.6");
		}

		{
			String r1_tpl = "{0}: Hello {1}, welcome to the {2} world. now is {3}, next is {4}, latest is {5}";
			String r1Expected = "TEST: Hello jiefzz, welcome to the java world. now is 512, next is 4096, latest is 16384";
			String r1Result = StringHelper.fill(r1_tpl, "TEST", "jiefzz", "java", 512, 4096, 16384, "你好", "世界");

			Assertions.assertEquals(r1Expected, r1Result, "formatTest.7");
		}

		{
			String r1_tpl = "Hello {}, welcome to the {} world. We use \\{\\} to mark a occupa.";
			String r1Expected = "Hello jiefzz, welcome to the java world. We use {} to mark a occupa.";
			String r1Result = StringHelper.fill(r1_tpl, "jiefzz", "java", 5354);
			
			Assertions.assertEquals(r1Expected, r1Result, "formatTest.8");
		}

		{
			String r1_tpl = "\\{Hello {}, welcome to the {} world. We use \\{\\} to mark a occupa.\\}";
			String r1Expected = "{Hello jiefzz, welcome to the java world. We use {} to mark a occupa.}";
			String r1Result = StringHelper.fill(r1_tpl, "jiefzz", "java", 5354);
			
			Assertions.assertEquals(r1Expected, r1Result, "formatTest.9");
		}

		{
			String r1_tpl = "\\{Hello {}, welcome to the {} world. We use \\{\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\} to mark a occupa.\\}";
			String r1Expected = "{Hello jiefzz, welcome to the java world. We use {}}}}}}}}}}}}}}}}} to mark a occupa.}";
			String r1Result = StringHelper.fill(r1_tpl, "jiefzz", "java", 5354);
			
			Assertions.assertEquals(r1Expected, r1Result, "formatTest.10");
		}

		{
			String r1_tpl = "Hello {}, welcome to the {} world. We use \\{\\} to mark a occupa. next value: {}.";
			String r1Expected = "Hello jiefzz, welcome to the java world. We use {} to mark a occupa. next value: 5354.";
			String r1Result = StringHelper.fill(r1_tpl, "jiefzz", "java", 5354);
			
			Assertions.assertEquals(r1Expected, r1Result, "formatTest.11");
		}

		{
			String r1_tpl = "Hello {}, welcome to the {} world. next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}.";
			String r1Expected
			 = "Hello jiefzz, welcome to the java world. next 1234, next 2345, next 3456, next 4567, next 5678, next 6789, next 7890, next 1, next 2, next 3, next 4, next 5, next 6, next 7.";
			String r1Result = StringHelper.fill(r1_tpl, "jiefzz", "java", 1234, 2345, 3456, 4567, 5678, 6789, 7890, 1, 2, 3, 4, 5, 6, 7);
			
			Assertions.assertEquals(r1Expected, r1Result, "formatTest.12");
		}

		{
			Assertions.assertThrows(RuntimeException.class, () -> {
				String r1_tpl = "Hello {, welcome to the {} world.";
				StringHelper.fill(r1_tpl, "jiefzz", "java");
			}, "formatTest.13.RuntimeExtion");
		}

		{
			String r1_tpl = "null is {}. a new value is {}.";
			String r1Expected = "null is . a new value is .";
			String r1Result = StringHelper.fill(r1_tpl, null, null);
			
			Assertions.assertEquals(r1Expected, r1Result, "formatTest.14");
		}

		{
			String nullValue = null;
			String r1_tpl = "null is {}.";
			String r1Expected = "null is .";
			String r1Result = StringHelper.fill(r1_tpl, nullValue);
			
			Assertions.assertEquals(r1Expected, r1Result, "formatTest.15");
		}

		{
			String r1_tpl = "null is {}.";
			String r1Expected = "null is .";
			String r1Result = StringHelper.fill(r1_tpl, null);
			
			Assertions.assertEquals(r1Expected, r1Result, "formatTest.16");
		}

		{
			String nullValue = null;
			String r1_tpl = "{} is a null value.";
			String r1Expected = " is a null value.";
			String r1Result = StringHelper.fill(r1_tpl, nullValue);
			
			Assertions.assertEquals(r1Expected, r1Result, "formatTest.17");
		}
	}

//	@Test
	public void TimeUseTest() {
		
		int loopAmount = 1000000;
		{
			String tpl = "Hello {}, welcome to the {} world. here \\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\} ok";
			StringHelper.fill(tpl, "jiefzz", "java");
			long ts = System.currentTimeMillis();
			for(int i=0; i<loopAmount; i++) {
				StringHelper.fill(tpl, "jiefzz", "java");
			}
			long te = System.currentTimeMillis();
			System.err.println(StringHelper.fill("Time use: {}ms.", te-ts));
			
			tpl = "Hello %s, welcome to the %s world. here \\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\} ok";
			ts = System.currentTimeMillis();
			for(int i=0; i<loopAmount; i++) {
				String.format(tpl, "jiefzz", "java");
			}
			te = System.currentTimeMillis();
			System.err.println(StringHelper.fill("xTime use: {}ms.", te-ts));
		}
		
		{
			String tpl = "AHello {}, welcome to the {} world. here \\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\}\\} ok";
			long ts = System.currentTimeMillis();
			for(int i=0; i<loopAmount; i++) {
				StringHelper.fill(tpl, "jiefzz", "java");
			}
			long te = System.currentTimeMillis();
			System.err.println(StringHelper.fill("Time use: {}ms.", te-ts));
		}

		{
			String r1_tpl = "Hello {}, welcome to the {} world. next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}.";
			StringHelper.fill(r1_tpl, "jiefzz", "java", 1234, 2345, 3456, 4567, 5678, 6789, 7890, 1, 2, 3, 4, 5, 6, 7);
			long ts = System.currentTimeMillis();
			for(int i=0; i<loopAmount; i++) {
				StringHelper.fill(r1_tpl, "jiefzz", "java", 1234, 2345, 3456, 4567, 5678, 6789, 7890, 1, 2, 3, 4, 5, 6, 7);
			}
			long te = System.currentTimeMillis();
			System.err.println(StringHelper.fill("Time use: {}ms.", te-ts));
		}

		{
			String r1_tpl = "AHello {}, welcome to the {} world. next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}, next {}.";
			long ts = System.currentTimeMillis();
			for(int i=0; i<loopAmount; i++) {
				StringHelper.fill(r1_tpl, "jiefzz", "java", 1234, 2345, 3456, 4567, 5678, 6789, 7890, 1, 2, 3, 4, 5, 6, 7);
			}
			long te = System.currentTimeMillis();
			System.err.println(StringHelper.fill("Time use: {}ms.", te-ts));

			r1_tpl = "AHello %s, welcome to the %s world. next %d, next %d, next %d, next %d, next %d, next %d, next %d, next %d, next %d, next %d, next %d, next %d, next %d, next %d.";
			ts = System.currentTimeMillis();
			for(int i=0; i<loopAmount; i++) {
				String.format(r1_tpl, "jiefzz", "java", 1234, 2345, 3456, 4567, 5678, 6789, 7890, 1, 2, 3, 4, 5, 6, 7);
			}
			te = System.currentTimeMillis();
			System.err.println(StringHelper.fill("xTime use: {}ms.", te-ts));
		}
	}
}
