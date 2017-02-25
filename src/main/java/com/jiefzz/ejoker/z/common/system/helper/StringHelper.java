package com.jiefzz.ejoker.z.common.system.helper;

public final class StringHelper {

	public static boolean isNullOrEmpty(String targetString) {
		return (null==targetString || "".equals(targetString))?true:false;
	}
	
	public static boolean isNullOrWhiteSpace(String targetString) {
		if(null==targetString)
			return true;
		return "".equals(targetString.trim())?true:false;
	}
}
