package pro.jiefzz.ejoker.common.system.enhance;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class StringUtilx {

	public static boolean isNullOrEmpty(String targetString) {
		return null==targetString || "".equals(targetString);
	}
	
	public static boolean notEmpty(String targetString) {
		return null!=targetString && !"".equals(targetString);
	}
	
	public static boolean isNullOrWhiteSpace(String targetString) {
		return null==targetString || "".equals(targetString.trim());
	}
	
	public static boolean isSenseful(String targetString) {
		return null!=targetString && !"".equals(targetString.trim());
	}
	
	public static byte[] getBytes(String data, String charsetName) {
		try {
			return data.getBytes(charsetName);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 实现类似 slf4j 的占位填充功能。用以替换 String.format
	 * @param template
	 * @param args
	 * @return
	 */
	public static String fill(String template, Object... args) {
		if(null == template || "".equals(template))
			return "";
		
		MarkTuple analyzeOccupation = analyzeOccupation(template);
		
		if(null == args)
			args = emptyArgs;
		char[] chars = template.toCharArray();
		
		int esIndex = -1;
		int[] escapes = analyzeOccupation.escapes;
		if(null != escapes && 0 < escapes.length)
			esIndex = 0;

		int[] occupation = analyzeOccupation.marks;
		StringBuilder sb = new StringBuilder();
		int ll = -2;
		int lr = -1;
		int i = 0;
		do {
			ll = occupation[2*i];
			if(esIndex>=0) {
				int latestEsPoint = escapes[esIndex];
				if(latestEsPoint >= lr+1 && latestEsPoint < ll) {
					int startAt = lr+1;
					do {
						sb.append(chars, startAt, latestEsPoint-startAt);
						startAt = latestEsPoint+1;
					} while((latestEsPoint = escapes[++esIndex]) >= lr+1 && escapes[esIndex] < ll);
					sb.append(chars, startAt, ll-startAt);
				} else {
					sb.append(chars, lr+1, ll-lr-1);
				}
				if(latestEsPoint<0)
					esIndex = -1;
			} else
				sb.append(chars, lr+1, ll-lr-1);
			// if ll +1 != lr ??
			// TODO 有特殊功能要实现不？
			lr = occupation[2*i+1];
			if(lr < 0)
				break;
			if(i < args.length && null != args[i])
				sb.append(args[i]);
			else if(i >= args.length) {
				if(lr>0)
					sb.append(chars, lr-1, chars.length-lr+1);
			}
		} while (i++ < args.length);
		return sb.toString();
	}
	
	//{}
	// "This is a value of {}, name is {}, review."
	//  0....5....0....5....0....5....0....5....0....5....0
	//           10        20        30        40        50 
	
	private final static Map<String, MarkTuple> CacheMarks = new ConcurrentHashMap<>();
	
	private final static int appendSize = 16;
	
	private final static Object[] emptyArgs = new Object[] {null};
	
	private final static MarkTuple analyzeOccupation(String template) {
		return MapUtil.getOrAdd(CacheMarks, template, () -> {
			char[] chars = template.toCharArray();
			char latestSymbol = (char )0;
			int[] marks = new int[appendSize];
			int[] escapes = null;
			int index = 0;
			int indexEscape = 0;
			for(int i=0; i<chars.length; i++) {
				char currC = chars[i];
				if('{' == currC || '}' == currC) {
					if(i>0 && '\\' == chars[i-1]) {
						if(null == escapes)
							escapes = new int[appendSize];
						else if(indexEscape == escapes.length)
							escapes = generalNewMarks(escapes, appendSize); // 扩容
						escapes[indexEscape++] = i-1;
						continue;
					}
					if(latestSymbol == currC)
						throw new RuntimeException("Unexpect string template!!! index: " + i + ", template: "+ template);
					latestSymbol = currC;
					marks[index++] = i;
					if(0 == index%appendSize)
						marks = generalNewMarks(marks, appendSize); // 扩容
				}
			}
			if(0 != index%2)
				throw new RuntimeException("Unexpect string template!!! unmatch '{' and '}', template: "+ template);
			if(index == marks.length) {
				marks = generalNewMarks(marks, 2); // 扩容
			}
			marks[index++] = chars.length;
			for(int j = index; j<marks.length; j++)
				marks[j] = -1;
			
			if(null != escapes) {
				for(int k = indexEscape; k<escapes.length; k++)
					escapes[k] = -1;
				return new MarkTuple(marks, escapes);
			}
			
			return new MarkTuple(marks);
		});
	}
	
	private final static int[] generalNewMarks(int[] prevous, int appendSize) {
		int[] marksNew = new int[prevous.length + appendSize];
		System.arraycopy(prevous, 0, marksNew, 0, prevous.length);
		return marksNew;
	}
	
	private final static class MarkTuple {
		
		private int[] marks;
		
		private int[] escapes;
		
		public MarkTuple(int[] marks, int[] escapes) {
			this.marks = marks;
			this.escapes = escapes;
		}
		
		public MarkTuple(int[] marks) {
			this.marks = marks;
			this.escapes = null;
		}
	}
}
