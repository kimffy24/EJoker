package com.jiefzz.ejoker.z.common.system.helper;

public class StringHelper {

	public static boolean isNullOrEmpty(String targetString) {
		if(null==targetString) return true;
		if("".equals(targetString)) return true;
		return false;
	}
}
