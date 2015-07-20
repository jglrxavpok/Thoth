import org.jglr.thoth.Constants;
import org.jglr.thoth.Thoth;
import org.jglr.thoth.compiler.JVMCompiler;
import org.jglr.thoth.parser.ThothClass;
import org.jglr.thoth.parser.ThothParserException;
import org.junit.Assert;
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
