package com.jiefzz.ejoker.z.common.utilities;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import com.jiefzz.ejoker.z.common.utilities.GenericDefination.GenericDefinationRef;

public class GenericDefinedTypeMeta extends GenericDefinationRef {

	public final Type regionTye;

	public final String TypeName;

	public final int level;

	public final boolean isGeneric;

	public final boolean isArray;

	public final Class<?> rawType;

	public final GenericDefinedTypeMeta[] deliveryTypeMetasTable;

	public final boolean isWildcardType;

	public final GenericDefinedTypeMeta[] boundsUpper;

	public final GenericDefinedTypeMeta[] boundsLower;

	protected GenericDefinedTypeMeta(Type regionTye, GenericDefination referMeta, int level) {
		super(referMeta);
		this.regionTye = regionTye;
		this.TypeName = regionTye.getTypeName();
		this.level = level;
		
		if (Class.class.equals(regionTye.getClass())) {
			isGeneric = false;
			deliveryTypeMetasTable = null;
			isWildcardType = false;
			boundsUpper = null;
			boundsLower = null;
			Class<?> regionClazz = (Class<?> )regionTye;
			if(regionClazz.isArray()) {
				// TODO case1: type is a common array
				isArray = true;
				rawType = regionClazz.getComponentType();
			} else {
				// TODO case2: type is a common class
				isArray = false;
				rawType = regionClazz;
			}
		} else if(regionTye instanceof ParameterizedType) {
			isGeneric = true;
			isArray = false;
			isWildcardType = false;
			boundsUpper = null;
			boundsLower = null;
			ParameterizedType pt = (ParameterizedType )regionTye;
			rawType = (Class<?> )pt.getRawType();
			Type[] actualTypeArguments = pt.getActualTypeArguments();
			deliveryTypeMetasTable = new GenericDefinedTypeMeta[actualTypeArguments.length];
			for(int i = 0; i<actualTypeArguments.length; i++) {
				deliveryTypeMetasTable[i] = new GenericDefinedTypeMeta(actualTypeArguments[i], referMeta, level + 1);
			}
		} else if(regionTye instanceof GenericArrayType) {
			isWildcardType = false;
			boundsUpper = null;
			boundsLower = null;
			isArray = true;
			GenericArrayType gat = (GenericArrayType )regionTye;
			Type genericComponentType = gat.getGenericComponentType();
			
			GenericDefinedTypeMeta tmpTypeMeta = new GenericDefinedTypeMeta(genericComponentType, referMeta, level + 1);
			{
				// TODO check the component type is WildcardType!!!!
				if(tmpTypeMeta.isWildcardType) {
					throw new RuntimeException("Do you ensure that this statement will happen???");
				}
			}
			isGeneric = tmpTypeMeta.isGeneric;
			rawType = tmpTypeMeta.rawType;
			deliveryTypeMetasTable = new GenericDefinedTypeMeta[tmpTypeMeta.deliveryTypeMetasTable.length];
			System.arraycopy(tmpTypeMeta.deliveryTypeMetasTable, 0, deliveryTypeMetasTable, 0, tmpTypeMeta.deliveryTypeMetasTable.length);
		} else if(regionTye instanceof WildcardType) {
			isGeneric = false;
			isArray = false;
			rawType = null;
			deliveryTypeMetasTable = null;
			isWildcardType = true;
			WildcardType wt = (WildcardType )regionTye;
			{
				Type[] regionUpperBounds = wt.getUpperBounds();
				if(null == regionUpperBounds || 0 == regionUpperBounds.length) {
					boundsUpper = null;
				} else {
					boundsUpper = new GenericDefinedTypeMeta[regionUpperBounds.length];
					for(int i = 0; i<regionUpperBounds.length; i++) {
						boundsUpper[i] = new GenericDefinedTypeMeta(regionUpperBounds[i], referMeta, level + 1);
					}
				}
			}
			{
				Type[] regionLowerBounds = wt.getLowerBounds();
				if(null == regionLowerBounds || 0 == regionLowerBounds.length) {
					boundsLower = null;
				} else {
					boundsLower = new GenericDefinedTypeMeta[regionLowerBounds.length];
					for(int i = 0; i<regionLowerBounds.length; i++) {
						boundsLower[i] = new GenericDefinedTypeMeta(regionLowerBounds[i], referMeta, level + 1);
					}
				}
			}
		} else {
			throw new RuntimeException("Do you ensure that this statement will happen???");
		}
	}
	
	public GenericDefinedTypeMeta(Type regionTye, GenericDefination referMeta) {
		this(regionTye, referMeta, 0);
	}

}