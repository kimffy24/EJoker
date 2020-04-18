package pro.jiefzz.ejoker.test.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pro.jk.ejoker.common.utils.ClassNamesScanner;

class T5ClassNamesScanner {

	private final static String packageName1;
	private final static String packageName2;
	
	static {
		packageName1 = "pro.jiefzz.ejoker.test.ins";
		packageName2 = "pro.jiefzz.ejoker.test.ins2";
	}
	
	@Test
	void testScanClass() {
		System.out.println("scan package: " + packageName1 + ", " + packageName2);
		List<String> scan = ClassNamesScanner.scan(packageName1);
	}

	@Test
	@DisplayName("ClassNamesScanner.scan")
	void testScan() {
		System.out.println("scan package: " + packageName1);
		List<String> scan = ClassNamesScanner.scan(packageName1);
		int cs = (int )'A';
		for(int i=0; i<6; i++) {
			assertTrue(scan.contains(packageName1 + ".Class" + ((char )(cs+i))), "Class" + ((char )(cs+i)));
		}
		
		int is = (int )'1';
		for(int i=0; i<3; i++) {
			assertTrue(scan.contains(packageName1 + ".Interface" + ((char )(is+i))), "Interface" + ((char )(is+i)));
		}
	}

}
