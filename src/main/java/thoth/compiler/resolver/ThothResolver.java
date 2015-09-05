package thoth.compiler.resolver;

import thoth.Utils;
import thoth.compiler.CompilerOptions;
import thoth.compiler.ThothCompilePhase;
import thoth.compiler.ThothType;
import thoth.compiler.parser.ParsedClass;
import thoth.compiler.parser.ParsedFunction;
import thoth.compiler.parser.ThothParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * ThothResolver's role is to find the appropriate types for function types, imports and type definition.<br/>
 *
 */
public class ThothResolver extends ThothCompilePhase {

    private static ResolvedClass[] baseClasses;
    public static ResolvedClass[] getBaseClasses() {
        if(baseClasses == null) {
            try {
                ParsedClass typeClass = parse("thoth.lang.ThothTypes");
                ParsedClass operationsClass = parse("thoth.lang.Operations");

                ThothResolver resolver = createEmpty(CompilerOptions.copyDefault(), typeClass, operationsClass);
                resolver.resolve();
                baseClasses = resolver.getResolvedClasses();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return baseClasses;
    }

    private static ParsedClass parse(String fullClassName) throws IOException {
        String code = Utils.readString(ThothResolver.class.getResourceAsStream("/"+fullClassName.replace('.', '/')+".th"), "UTF-8");
        ThothParser parser = new ThothParser(code, fullClassName);
        parser.parse();
        printAllWarningsAndErrors(parser);
        return parser.getParsedClass();
    }

    private static void printAllWarningsAndErrors(ThothCompilePhase parser) {
        parser.getWarnings().forEach(Throwable::printStackTrace);
        parser.getErrors().forEach(Throwable::printStackTrace);
    }

    public static ThothResolver createWithDefaultClasses(CompilerOptions options, ParsedClass... classes) {
        return new ThothResolver(getBaseClasses(), options, classes);
    }

    public static ThothResolver createWithDefaultClasses(ParsedClass... classes) {
        return createWithDefaultClasses(CompilerOptions.copyDefault(), classes);
    }

    public static ThothResolver createEmpty(CompilerOptions options, ParsedClass... classes) {
        return new ThothResolver(new ResolvedClass[0], options, classes);
    }

    private final CompilerOptions options;
    private final ResolvedClass[] resolvedClasses;
    private final ParsedClass[] parsed;
    private final ResolvedClass[] referenceClasses;
    private final List<ThothType> types;

    public ThothResolver(ResolvedClass[] referenceClasses, CompilerOptions options, ParsedClass... classes) {
        this.options = options;
        types = new ArrayList<>();
        this.referenceClasses = referenceClasses;
        addTypes(referenceClasses);
        this.parsed = classes;
        resolvedClasses = new ResolvedClass[classes.length];
    }

    private List<ThothType> getTypes() {
        return types;
    }

    private void addTypes(ResolvedClass[] classes) {
        for(final ResolvedClass c : classes) {
            for(final ThothType userType : c.getUserTypes()) {
                types.forEach(t -> {
                    if(t.equals(userType)) {
                        newWarning("redefining.type", "Redefining type "+t.getName(), -1, -1); // TODO: Proper line/column
                    }
                });
                types.add(userType);
            }
        }
    }

    public void resolve() {
        for (int i = 0; i < parsed.length; i++) {
            ParsedClass parsedClass = parsed[i];
            List<ResolvedClass> imports = new ArrayList<>();
            for(ResolvedClass ref : referenceClasses) {
                imports.add(ref);
            }
            List<ResolvedFunction> functions = new ArrayList<>();
            ResolvedClass resolvedClass = new ResolvedClass(parsedClass.getName(), parsedClass.getSourceFile(), imports, parsedClass.getUserTypes(), functions, parsedClass.getClassType());
            resolvedClasses[i] = resolvedClass;
        }

        addTypes(resolvedClasses);

        for (int i = 0; i < parsed.length; i++) {
            ParsedClass parsedClass = parsed[i];
            ResolvedClass resolved = resolvedClasses[i];
            List<String> imports = parsedClass.getImports();
            for(String s : imports) {
                ResolvedClass imported = getByName(s);
                if(imported == null) {
                    newError("Cannot resolve import of "+s, -1, -1);
                }
                resolved.getImports().add(imported);
            }

            for(ResolvedClass companion : resolvedClasses) {
                if(companion != resolved)
                    resolved.getImports().add(companion);
            }

            List<ParsedFunction> functions = parsedClass.getFunctions();
            for(ParsedFunction f : functions) {
                ThothType[] resolvedTypes = new ThothType[f.getTypes().length];
                for(int j = 0;j<resolvedTypes.length;j++) {
                    String typeName = f.getTypes()[j];
                    ThothType type = getType(typeName);
                    if(type == null) {
                        newError("Cannot resolve type "+typeName+" for function "+f.getName(), -1, -1);
                    }
                    resolvedTypes[j] = type;
                }
                ResolvedFunction resolvedFunction = new ResolvedFunction(f.getName(), f.getArgumentNames(), resolvedTypes, f.getCode(), resolved);
                resolved.getFunctions().add(resolvedFunction);
            }
        }
    }

    private ThothType getType(String name) {
        for(ThothType t : types) {
            if(t.getName().equals(name))
                return t;
        }
        return null;
    }

    private ResolvedClass getByName(String name) {
        for(ResolvedClass c : referenceClasses) {
            if(name.equals(c.getName()))
                return c;
        }

        for(ResolvedClass c : resolvedClasses) {
            if(name.equals(c.getName()))
                return c;
        }
        return null;
    }

    public ResolvedClass[] getResolvedClasses() {
        return resolvedClasses;
    }
}
