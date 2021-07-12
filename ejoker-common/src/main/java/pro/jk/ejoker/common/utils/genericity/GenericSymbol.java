package pro.jk.ejoker.common.utils.genericity;

import java.lang.reflect.TypeVariable;

/**
 * 
 * Represents a meta object for Generic Declare Symbol. <br />
 * includes the Generic Type declared name and the position of Generic Class attached and the raw TypeVariable&lt;?&gt; object. <br /><br />
 * 
 * 记录泛型类型中的一些泛型变量信息。<br />
 * 其中包括泛型变量的名字和定义所在位置信息 <br /><br />
 * 
 * @author kimffy
 *
 */
public class GenericSymbol extends GenericDefinitionElement {
	
	public final int index;
	
	public final String name;
	
	public final TypeVariable<?> typeVar;

	public GenericSymbol(GenericDefinition ref, int index, TypeVariable<?> typeVar) {
		super(ref);
		this.index = index;
		this.typeVar = typeVar;
		this.name = typeVar.getName();
	}
	
	public boolean isSameGenericDefination(GenericDefinition ref) {
		return this.referDefination.equals(ref);
	}

	@Override
	public String toString() {
		return this.referDefination.genericPrototypeClazz.getName() + "." + name + "(#" + index + ")";
	}

}
