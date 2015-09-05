package thoth.compiler;

import thoth.compiler.bytecode.ThothCompiler;
import thoth.compiler.parser.ParsedClass;
import thoth.compiler.parser.ThothParser;
import thoth.compiler.resolver.ResolvedClass;
import thoth.compiler.resolver.ThothResolver;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class FullCompiler {

    private final CompilerOptions options;

    public FullCompiler(CompilerOptions options) {
        this.options = options;
    }

    public Collection<byte[]> compile(URL... sourceFile) throws IOException {
        ParsedClass[] parsed = new ParsedClass[sourceFile.length];
        for(int i = 0;i<sourceFile.length;i++) {
            parsed[i] = parse(sourceFile[i]);
        }
        ThothResolver resolver = ThothResolver.createWithDefaultClasses(options, parsed);
        resolver.resolve();
        printAllWarningsAndErrors(resolver);
        ThothCompiler compiler = new ThothCompiler();
        List<byte[]> result = new LinkedList<>();
        for(ResolvedClass clazz : resolver.getResolvedClasses()) {
            byte[] bytecode = compiler.compile(clazz);
            result.add(bytecode);
        }
        return result;
    }

    private ParsedClass parse(URL url) throws IOException {
        InputStream in = new BufferedInputStream(url.openStream());
        int i;
        byte[] buffer = new byte[options.getBufferSize()];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while((i = in.read(buffer)) != -1) {
            out.write(buffer, 0, i);
        }
        out.flush();
        out.close();
        String code = new String(out.toByteArray(), options.getEncoding());
        String path = url.getPath();
        String sourceFile = path.substring(path.lastIndexOf('/')+1);
        ThothParser parser = new ThothParser(code, sourceFile, options);
        return parser.getParsedClass();
    }

    private void printAllWarningsAndErrors(ThothCompilePhase parser) {
        parser.getWarnings().forEach(Throwable::printStackTrace);
        parser.getErrors().forEach(Throwable::printStackTrace);
    }
}
