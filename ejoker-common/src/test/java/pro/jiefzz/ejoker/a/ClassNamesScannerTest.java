package pro.jiefzz.ejoker.a;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pro.jiefzz.ejoker.a.ins.ClassF;
import pro.jiefzz.ejoker.a.ins2.ClassG;
import pro.jk.ejoker.common.utils.ClassNamesScanner;

class ClassNamesScannerTest {

	private final static String packageName1;
	private final static String packageName2;
	
	static {
		packageName1 = "pro.jiefzz.ejoker.a.ins";
		packageName2 = "pro.jiefzz.ejoker.a.ins2";
	}

	@BeforeEach
	public void everyBefore() {
		System.out.println("==================== ");
	}
	
	@Test
	void testScanClass() {
		System.out.println("scan package: " + packageName1);
		List<String> scan = ClassNamesScanner.scan(packageName1);
		Assertions.assertTrue(scan.contains(ClassF.class.getName()));
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
	
	@Test
	void testScanClass2() {
		System.out.println("scan package: " + packageName2);
		List<String> scan = ClassNamesScanner.scan(packageName2);
		Assertions.assertTrue(scan.contains(ClassG.class.getName()));
		Assertions.assertFalse(scan.contains(ClassF.class.getName()));
	}

}
