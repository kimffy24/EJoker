package pro.jiefzz.ejoker.utils;
//package com.jiefzz.ejoker.utils;
//
//import java.lang.management.ManagementFactory;
//import java.net.NetworkInterface;
//import java.nio.ByteBuffer;
//import java.security.SecureRandom;
//import java.util.Enumeration;
//
//public class EObjectId {
//
//	private static enum BinarySystem {
//		Hexadecimal,
//		Duotricemary,
//		SixtyFourBinarySystem
//	};
//	private static final short TS_SIZE=6;
//	private static final short MI_SIZE=16;
//	private static final short PI_SIZE=16;
//	private static final short RD_SIZE=4;
//
//	private static final long MACHINE_IDENTIFIER;
//	private static final int PROCESS_IDENTIFIER;
//
//	private static final char[] HEX_MI = new char[MI_SIZE];
//	private static final char[] BS32_MI = new char[MI_SIZE];
//	private static final char[] BS64_MI = new char[MI_SIZE];
//
//	private static final char[] HEX_PI = new char[PI_SIZE];
//	private static final char[] BS32_PI = new char[PI_SIZE];
//	private static final char[] BS64_PI = new char[PI_SIZE];
//
//	private static final char BS062_CHAR = '_';
//	private static final char BS063_CHAR = '-';
//	private static final char[] BS64_CHARS = new char[] {
//			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
//			'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
//			'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
//			BS062_CHAR, BS063_CHAR };
//	private static final char[] BS32_CHARS = new char[] {
//			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
//			'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v' };
//	private static final char[] HEX_CHARS = new char[] {
//			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
//			'a', 'b', 'c', 'd', 'e', 'f' };
//
//	private static long seed;
//	
//	private long timestamp;
//	private long randomDivision;
////	private String holderStringHex;
////	private String holderStringBS32;
////	private String holderStringBS64;
//	
//	private EObjectId() {
//		timestamp = System.currentTimeMillis()*10;
//		randomDivision = disposableSeed();
//	}
//
//	public String toString() {
//		return toBS32String();
//	}
//	
//	public String toHexString(){
//		BinarySystem binarySystem = BinarySystem.Hexadecimal;
//		char[] tsArray=new char[MI_SIZE];
//		char[] rdArray=new char[MI_SIZE];
//		cvtr(timestamp, binarySystem, tsArray);
//		cvtr(randomDivision, binarySystem, rdArray);
//		String timePrefix = trimString(tsArray);
//		String mToken = trimString(HEX_MI, 6);
//		String pToken = trimString(HEX_PI, 4);
//		String sToken = trimString(rdArray,4);
//		return timePrefix+mToken+pToken+sToken;
//	}
//
//	public String toBS32String(){
//		throw new RuntimeException("Unimplemented!");
//	}
//	public String toBS64String(){
//		throw new RuntimeException("Unimplemented!");
//	}
//
//	public String toDuotricemaryString() {
//		throw new RuntimeException("Unimplemented!");
//	}
//
//	public static final String generateHexStringId(){
//		return (new EObjectId()).toHexString();
//	}
//	
//	synchronized private static long disposableSeed() {
//		return ++seed;
//	}
//
//	public static long createMachineIdentifier() {
//		// build machine piece based on NICs info
//		try {
//			int[] nInfo = {0, 0, 0};
//			Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
//			while (e.hasMoreElements()) {
//				NetworkInterface ni = e.nextElement();
//				byte[] mac = ni.getHardwareAddress();
//				if (mac != null) {
//					ByteBuffer bb = ByteBuffer.wrap(mac);
//					for ( int i=0; i<3; i++)
//						nInfo[i]+=(int) (bb.getShort()&0xffff);
//				}
//			}
//			long x = (long )nInfo[0];
//			long y = (long )nInfo[1];
//			long z = (long )nInfo[2];
//			return (x<<17 | y<<9 | z) & 0xffffffffffffffffL;
//		} catch (Throwable t) {
//			t.printStackTrace();
//			// exception sometimes happens with IBM JVM, use random
//			return (new SecureRandom().nextLong());
//		}
//	}
//
//	private static int createProcessIdentifier() {
//		try {
//			String processName = ManagementFactory.getRuntimeMXBean().getName();
//			return (processName.contains("@"))?
//					Integer.parseInt(processName.substring(0, processName.indexOf('@'))):
//						processName.hashCode();
//		} catch (Throwable t) {
//			return (new SecureRandom()).nextInt();
//		}
//
//	}
//
//	private static void cvtr(long targetNumber, BinarySystem binarySystem, char[] referArray, int round){
//		if( round == referArray.length ) return;
//		//int offset = referArray.length - round - 1;
//		int offset = round;
//		char[] referDict = null;
//		int sLimit;
//		switch (binarySystem) {
//		case Hexadecimal:
//			referDict=HEX_CHARS;
//			sLimit = 16;
//			break;
//		case Duotricemary:
//			referDict=BS32_CHARS;
//			sLimit = 32;
//			break;
//		case SixtyFourBinarySystem:
//			referDict=BS64_CHARS;
//			sLimit = 64;
//			break;
//		default :
//			throw new RuntimeException("Unknow Binary System to convert!!!");
//		}
//		if(sLimit>targetNumber) {
//			referArray[offset]=referDict[(int) targetNumber];
//			//for (int i=round+1; i<referArray.length; i++ ) referArray[i]=(char )0;
//			return;
//		}
//		int remainder = (int) (targetNumber%sLimit);
//		long divider = (long) (targetNumber/sLimit);
//		referArray[offset]=referDict[remainder];
//		cvtr(divider, binarySystem, referArray, ++round);
//	}
//	
//	private static void cvtr(long targetNumber, BinarySystem binarySystem, char[] referArray){
//		cvtr(targetNumber, binarySystem, referArray, 0);
//	}
//
//	private static String trimString(char[] toTrim){
//		StringBuilder sb = new StringBuilder();
//		for (char c : toTrim)
//			if (c!=0)
//				sb.append(c);
//		return sb.toString();
//	}
//	
//	private static String trimString(char[] toTrim, int max){
//		StringBuilder sb = new StringBuilder();
//		int i = 0;
//		for (char c : toTrim)
//			if (c!=0 && i++ < max) {
//				sb.append(c);
//			}
//		return sb.toString();
//	}
//	
//	static {
//		MACHINE_IDENTIFIER=createMachineIdentifier();
//		PROCESS_IDENTIFIER=createProcessIdentifier();
//		
//		BinarySystem[] bsArray = {BinarySystem.Hexadecimal, BinarySystem.Duotricemary, BinarySystem.SixtyFourBinarySystem};
//		char[][][] scik = {{HEX_MI, HEX_PI}, {BS32_MI, BS32_PI}, {BS64_MI, BS64_PI}};
//		for ( int i=0; i<3; i++ ){
//			cvtr(MACHINE_IDENTIFIER, bsArray[i], scik[i][0]);
//			cvtr(PROCESS_IDENTIFIER, bsArray[i], scik[i][1]);
//		}
//		seed = (new SecureRandom().nextLong());
//		while (  seed < 0xFFFFFFL )
//			seed = (new SecureRandom().nextLong());
//	}
//
//}
