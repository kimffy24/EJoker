package pro.jk.ejoker.common.utils.genericity;

/**
 * 
 * Represents a object must belong to a {@link GenericDefinition} object.<br /><br />
 * 
 * 继承此基类标明当前元素必须在某个{@link GenericDefinition}下才有意义<br /><br />
 * 
 * @author kimffy
 *
 */
public abstract class GenericDefinitionElement {
	
	protected final GenericDefinition referDefination;
	
	protected GenericDefinitionElement(GenericDefinition referDefination) {
		this.referDefination = referDefination;
	}
	
	public GenericDefinition getGenericDefination() {
		return referDefination;
	}
	
}
