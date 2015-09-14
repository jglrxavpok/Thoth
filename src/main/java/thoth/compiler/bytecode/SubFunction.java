package thoth.compiler.bytecode;

import thoth.compiler.ThothType;
import thoth.compiler.bytecode.instructions.ThothInstruction;

import java.util.ArrayList;
import java.util.List;

public class SubFunction extends CompiledFunction {

    SubFunction(String name, String[] arguments) {
        super(name, arguments, new ThothType[0], new ArrayList<>(), new ArrayList<>());
    }
}
