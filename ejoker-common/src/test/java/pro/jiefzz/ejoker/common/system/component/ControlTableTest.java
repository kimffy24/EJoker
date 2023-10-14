package pro.jiefzz.ejoker.common.system.component;

import static pro.jk.ejoker.common.system.component.ControlTable.RegisterTuple.of;

import org.junit.jupiter.api.Test;
import pro.jk.ejoker.common.system.component.ControlTable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ControlTableTest {

    @Test
    public void testCreate() {

        {
            ControlTable.provideRegisterTuple(
                    of("author.name", "E_AUTHOR_NAME", "jiefzz24", "姓名"),
                    of("author.age", "E_AUTHOR_AGE", "32", "年龄"),
                    of("author.mf", "E_AUTHOR_MF", "1", "性别"),
                    of("author.sex", "E_AUTHOR_SEX", "1", "性别")
            );


            Set<String> booleanTest = new HashSet<>();
            booleanTest.add("true");
            booleanTest.add("True");
            booleanTest.add("false");
            booleanTest.add("False");
            booleanTest.add("T");
            booleanTest.add("t");
            booleanTest.add("F");
            booleanTest.add("f");
            booleanTest.add("0");
            booleanTest.add("1");

            ControlTable.provideRegisterTuple(
                    of("what.is.your.like", "", "beautify girls", "你喜欢啥？"),
                    of("test.this.is.a.number", "", "999", "测试1", "^[+-]?\\d+$"),
                    of("test.this.is.a.booleanValue", "", "1", "测试boolean", booleanTest::contains),
                    of("test.this.is.a.decimal", "", "1.1", "测试浮动", "^[0-9]+(\\.[0-9]*)?$")
            );
        }

        System.err.println(ControlTable.getConfigValue("author.name"));
        System.err.println(ControlTable.getConfigValue("author.sex"));
        System.err.println(ControlTable.detectAsInteger("test.this.is.a.number"));
        System.err.println(ControlTable.detectAsBoolean("test.this.is.a.booleanValue"));
        System.err.println(ControlTable.detectAsDouble("test.this.is.a.decimal"));

    }

}
