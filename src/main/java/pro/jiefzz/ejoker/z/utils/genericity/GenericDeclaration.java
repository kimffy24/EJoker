package pro.jiefzz.ejoker.z.utils.genericity;

/**
 * 
 * Represents a meta object for Generic Class. <br />
 * includes the Generic Type declared name and the position of Generic Class attached. <br /><br />
 * 
 * 记录泛型类型中的一些泛型变量信息。<br />
 * 其中包括泛型变量的名字和定义所在位置信息 <br /><br />
 * 
 * @author kimffy
 *
 */
public class GenericDeclaration extends GenericDefinationEssential {
	
	public final int index;
	
	public final String name;

	public GenericDeclaration(GenericDefination ref, int index, String name) {
		super(ref);
		this.index = index;
		this.name = name;
	}
	
	public boolean isSameGenericDefination(GenericDefination ref) {
		return this.referDefination.equals(ref);
	}

}
