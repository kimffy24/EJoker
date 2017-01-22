package com.jiefzz.ejoker.z.common.system.helper;

public class StringHelper {

	public static boolean isNullOrEmpty(String targetString) {
		return (null==targetString || "".equals(targetString))? true:false;
	}
}
