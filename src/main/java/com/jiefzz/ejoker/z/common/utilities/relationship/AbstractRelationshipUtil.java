package com.jiefzz.ejoker.z.common.utilities.relationship;

import com.jiefzz.ejoker.z.common.utils.relationship.SpecialTypeCodec;
import com.jiefzz.ejoker.z.common.utils.relationship.SpecialTypeCodecStore;

public abstract class AbstractRelationshipUtil extends AbstractTypeAnalyze {

	protected final SpecialTypeCodecStore<?> specialTypeCodecStore;
	
	protected AbstractRelationshipUtil(SpecialTypeCodecStore<?> specialTypeCodecStore) {
		this.specialTypeCodecStore = specialTypeCodecStore;
	}
	
	protected Object processWithUserSpecialCodec(Object value, Class<?> valueType, Class<?> fieldType) {
		if(null == specialTypeCodecStore)
			return null;
		
		SpecialTypeCodec fieldTypeCodec = specialTypeCodecStore.getCodec(fieldType);
		if(null == fieldTypeCodec)
			return null;
		
		/// 完全类型对等 or 泛型的情况
		if(valueType.equals(fieldType) || Object.class.equals(fieldType))
			return fieldTypeCodec.encode(value);
		
		return null;
	}
}
