package pro.jiefzz.ejoker.test.z.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pro.jiefzz.ejoker.z.utils.ClassNamesScanner;

class T5ClassNamesScanner {

	private final static String packageName;
	
	static {
		String pName = T5ClassNamesScanner.class.getName().replace(T5ClassNamesScanner.class.getSimpleName(), "");
		packageName = pName + "demo";
	}
	
	@Test
	void testScanClass() {
		
	}

	@Test
	@DisplayName("ClassNamesScanner.scan")
	void testScan() {
		List<String> scan = ClassNamesScanner.scan(packageName);
		int cs = (int )'A';
		for(int i=0; i<6; i++) {
			assertTrue(scan.contains(packageName + ".Class" + ((char )(cs+i))), "Class" + ((char )(cs+i)));
		}
		
		int is = (int )'1';
		for(int i=0; i<3; i++) {
			assertTrue(scan.contains(packageName + ".Interface" + ((char )(is+i))), "Interface" + ((char )(is+i)));
		}
	}

}
