package pro.jiefzz.ejoker.common.system.helper;

import java.io.UnsupportedEncodingException;

public final class StringHelper {

	public static boolean isNullOrEmpty(String targetString) {
		return (null==targetString || "".equals(targetString))?true:false;
	}
	
	public static boolean isNullOrWhiteSpace(String targetString) {
		return (null==targetString || "".equals(targetString.trim()))?true:false;
	}
	
	public static byte[] getBytes(String data, String charsetName) {
		try {
			return data.getBytes(charsetName);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
}
