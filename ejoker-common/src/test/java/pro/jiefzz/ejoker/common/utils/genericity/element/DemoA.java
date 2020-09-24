package pro.jiefzz.ejoker.common.utils.genericity.element;

import java.lang.reflect.Type;

import pro.jk.ejoker.common.utils.genericity.GenericExpression;
import pro.jk.ejoker.common.utils.genericity.GenericExpressionFactory;
import pro.jk.ejoker.common.utils.genericity.TypeRefer;

public class DemoA<T> {

	public GenericExpression test1() {
		
		TypeRefer<T> sToken = new TypeRefer<T>(){};
		
		Type type = sToken.getType();
		
		GenericExpression genericExpress = GenericExpressionFactory.getGenericExpress(type);
		
		return genericExpress;
		
	}
	
}
