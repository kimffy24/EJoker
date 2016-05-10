package com.jiefzz.ejoker.infrastructure.z.common.utilities;

import com.jiefzz.ejoker.infrastructure.z.common.ArgumentException;
import com.jiefzz.ejoker.infrastructure.z.common.ArgumentNullException;
import com.jiefzz.ejoker.infrastructure.z.common.ArgumentNullOrEmptyException;
import com.jiefzz.ejoker.infrastructure.z.common.ArgumentOutOfRangeException;

public class Ensure {
	 public static <T> void notNull(T argument, String argumentName) {
         if (argument == null)
             throw new ArgumentNullException(argumentName);
     }

     public static void notNullOrEmpty(String argument, String argumentName)
     {
         if (argument == null || "".equals(argument))
             throw new ArgumentNullOrEmptyException(argumentName);
     }

     public static void positive(int number, String argumentName)
     {
         if (number <= 0)
             throw new ArgumentOutOfRangeException(argumentName, "should be positive.");
     }

     public static void positive(long number, String argumentName)
     {
         if (number <= 0)
             throw new ArgumentOutOfRangeException(argumentName, " should be positive.");
     }

     public static void nonnegative(long number, String argumentName)
     {
         if (number < 0)
             throw new ArgumentOutOfRangeException(argumentName, " should be non negative.");
     }

     public static void nonnegative(int number, String argumentName)
     {
         if (number < 0)
             throw new ArgumentOutOfRangeException(argumentName, " should be non negative.");
     }

//     public static void NotEmptyGuid(Guid guid, String argumentName)
//     {
//         if (Guid.Empty == guid)
//             throw new ArgumentException(argumentName, argumentName + " shoud be non-empty GUID.");
//     }

     public static void equal(int expected, int actual, String argumentName) {
         if (expected != actual)
             throw new ArgumentException("["+argumentName+"] expected value: "+expected+", actual value: "+actual);
     }

     public static void Equal(long expected, long actual, String argumentName) {
         if (expected != actual)
             throw new ArgumentException("["+argumentName+"] expected value: "+expected+", actual value: "+actual);
     }

     public static void Equal(boolean expected, boolean actual, String argumentName) {
         if (expected != actual)
             throw new ArgumentException("["+argumentName+"] expected value: "+expected+", actual value: "+actual);
     }
}
