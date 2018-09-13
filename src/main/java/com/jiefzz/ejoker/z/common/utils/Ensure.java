package com.jiefzz.ejoker.z.common.utilities;

import com.jiefzz.ejoker.z.common.ArgumentException;
import com.jiefzz.ejoker.z.common.ArgumentNullException;
import com.jiefzz.ejoker.z.common.ArgumentNullOrEmptyException;
import com.jiefzz.ejoker.z.common.ArgumentOutOfRangeException;

/**
 * 对象化的断言方法
 * @author jiefzz
 *
 */
public class Ensure {
	
	/**
	 * 断言变量不为空
	 * @param argument
	 * @param argumentName
	 */
	public static <T> void notNull(T argument, String argumentName) {
		if (argument == null)
			throw new ArgumentNullException(argumentName);
	}

	/**
	 * 断言字符串变量不为空且不为空字符串
	 * @param argument
	 * @param argumentName
	 */
	public static void notNullOrEmpty(String argument, String argumentName)  {
		if (argument == null || "".equals(argument))
			throw new ArgumentNullOrEmptyException(argumentName);
	}

	/**
	 * 断言为正数
	 * @param number
	 * @param argumentName
	 */
	public static void positive(int number, String argumentName)  {
		if (number <= 0)
			throw new ArgumentOutOfRangeException(argumentName, "should be positive.");
	}
	/**
	 * 断言为正数
	 * @param number
	 * @param argumentName
	 */
	public static void positive(long number, String argumentName) {
		if (number <= 0)
			throw new ArgumentOutOfRangeException(argumentName, " should be positive.");
	}

	/**
	 * 断言为非负数
	 * @param number
	 * @param argumentName
	 */
	public static void nonnegative(long number, String argumentName)  {
		if (number < 0)
			throw new ArgumentOutOfRangeException(argumentName, " should be non negative.");
	}
	/**
	 * 断言为非负数
	 * @param number
	 * @param argumentName
	 */
	public static void nonnegative(int number, String argumentName) {
		if (number < 0)
			throw new ArgumentOutOfRangeException(argumentName, " should be non negative.");
	}

	//     public static void NotEmptyGuid(Guid guid, String argumentName)
	//     {
	//         if (Guid.Empty == guid)
	//             throw new ArgumentException(argumentName, argumentName + " shoud be non-empty GUID.");
	//     }

	/**
	 * 断言值相等
	 * @param expected
	 * @param actual
	 * @param argumentName
	 */
	public static <T> void equal(T expected, T actual, String argumentName) {
		if (null == expected && null == actual)
			return;
		if (null == expected || !expected.equals(actual))
			throw new ArgumentException("["+argumentName+"] expected value: "+expected+", actual value: "+actual);
	}
}
