package thoth.utils;

import org.objectweb.asm.MethodVisitor;
import thoth.compiler.bytecode.instructions.ThothInstruction;
import thoth.compiler.resolver.ResolvedFunction;

/**
 * Function-like component for handling and compilation of ThothInstructions to bytecode
 */
@FunctionalInterface
public interface InstructionHandler {

    /**
     * Compiles a given instruction into bytecode
     * @param function
     *                The function from which the instruction is
     * @param mv
     *          The MethodVisitor used to generate the bytecode
     * @param insn
     *            The instruction to compile
     * @return
     *         Changes in the stack, positive number to tell that n elements are added, negative for removed, 0 for no change.
     *         It actually does not track type so a function call with only one argument provides no change even though the internal stack has changed.
     */
    int compile(ResolvedFunction function, MethodVisitor mv, ThothInstruction insn);
}
