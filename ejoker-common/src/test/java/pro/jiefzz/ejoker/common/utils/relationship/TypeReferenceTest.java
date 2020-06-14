package pro.jiefzz.ejoker.common.utils.relationship;

import java.lang.reflect.Type;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TypeReferenceTest {

	@Test
	public void testRawType() {
		
		TypeReference<String> sToken = new TypeReference<String>(){};
		
		Type type = sToken.getType();
		
		Assertions.assertEquals(String.class, type);
		
	}

	@Test
	public void testGenericType() {
		
		TypeReference<List<String>> sToken = new TypeReference<List<String>>(){};
		
		Type type = sToken.getType();
		
		Assertions.assertEquals(List.class, type);
		
	}
	
}
