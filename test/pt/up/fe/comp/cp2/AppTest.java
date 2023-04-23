package pt.up.fe.comp.cp2;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;
import utils.ProjectTestUtils;

import java.util.Collections;

public class AppTest {

    @Test
    public void testHelloWorld() {
        var code = SpecsIo.getResource("pt/up/fe/comp/cp2/apps/HelloWorld.jmm");
        var jasminResult = TestUtils.backend(code, Collections.emptyMap());
        ProjectTestUtils.runJasmin(jasminResult, "Hello, World!");
    }

    @Test
    public void testSimple() {
        var code = SpecsIo.getResource("pt/up/fe/comp/cp2/apps/Simple.jmm");
        var jasminResult = TestUtils.backend(code, Collections.emptyMap());
        ProjectTestUtils.runJasmin(jasminResult, "30");
    }

    @Test
    public void customTestVarLookupField() {
        var code = SpecsIo.getResource("pt/up/fe/comp/cp2/apps/VarLookupField.jmm");
        var jasminResult = TestUtils.backend(code, Collections.emptyMap());
        ProjectTestUtils.runJasmin(jasminResult, "10");
    }

    @Test
    public void customTestVarLookupLocal() {
        var code = SpecsIo.getResource("pt/up/fe/comp/cp2/apps/VarLookupLocal.jmm");
        var jasminResult = TestUtils.backend(code, Collections.emptyMap());
        ProjectTestUtils.runJasmin(jasminResult, "10");
    }
}
