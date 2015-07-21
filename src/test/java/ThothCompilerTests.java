import thoth.Constants;
import thoth.Thoth;
import thoth.compiler.JVMCompiler;
import thoth.lang.ThothClass;
import thoth.parser.ThothParserException;
import org.junit.Test;

import java.io.IOException;

public class ThothCompilerTests implements Constants {

    @Test
    public void jvmCompile() throws IOException, ThothParserException {
        ThothClass clazz = Thoth.singleton.compile(getClass().getResource("test.th"));
        JVMCompiler compiler = new JVMCompiler();
        compiler.compile(clazz);
    }
}
