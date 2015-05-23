package org.jglr.thoth;

import org.jglr.thoth.insns.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThothParser implements Constants {

    /**
     * Are we inside or outside a function declaration?
     */
    private boolean inGlobalScope;

    /**
     * The current node's id
     */
    private int nodeID = 0;

    /**
     * The current level of indent in labels, i.e. N5, fifth label of level 0, N6-8, eighth label of level 1
     */
    private String nodeBase = "N";

    public ThothParser() {
        inGlobalScope = true;
    }

    /**
     * Parses a String written in the Thoth syntax
     * @param rawLang
     *          The String to parse
     * @return
     *          A ThothClass containing a list of functions if everything worked
     * @throws ThothParserException
     *          If any error happens during the parsing process (i.e. wrong syntax)
     */
    public ThothClass parseRaw(String rawLang) throws ThothParserException {
        inGlobalScope = true;
        Map<String, ThothFunc> functions = new HashMap<>();
        for(int i = 0;i<rawLang.length() && i >= 0;) {
            if (inGlobalScope) {
                String keyword = DEF_KEYWORD+" ";
                i = seek(rawLang, i, keyword)+keyword.length(); // After searching for the first occurence of 'def ' we put the cursor behind it to start reading the function name
                inGlobalScope = false;
            } else {
                // def nameOfFunc(arg0, arg1...):type=value
                int paramsStart = seek(rawLang, i, "("); // The index of the opening bracket for the params
                int typeDef = seek(rawLang, i, ":"); // The index of the type identifier
                int equalSign = seek(rawLang, i, "="); // The index of the 'definition operator'
                int typeFlag = handleType(rawLang, equalSign, typeDef); // Creates a flag on 32 bits representing the type of the result

                int nameEnd = equalSign;
                if(!dneois(equalSign, typeDef)) { // We have the type, we shouldn't read until the '='
                    nameEnd = typeDef;
                }
                String[] params = {};
                if(!dneois(equalSign, paramsStart)) { // Here, we handle the parameters of the function.
                    nameEnd = paramsStart; // We use this moment to cut the name *right* before the opening bracket
                    String rawParams = rawLang.substring(paramsStart+1, seek(rawLang, i, ")")); // Get the params
                    rawParams = rawParams.replace(" ", "");
                    params = rawParams.split(",");
                }

                String name = rawLang.substring(i, nameEnd);

                int end = seek(rawLang, equalSign, DEF_END); // Seeks the end of the translation
                if(end < 0) {
                    end = rawLang.length();
                }
                String rawCode = rawLang.substring(equalSign + 1, end);
                i = end+DEF_END.length(); // We move the cursor after the translation to keep reading the other ones

                Translation tr = new Translation(typeFlag, rawCode, params);
                List<ThothCommandment> instructions = interpret(rawCode, params);
                ThothFunc function = new ThothFunc(name, params.length, instructions, tr); // Create a function representing the code given as a translation.
                functions.put(name, function);
            }
        }
        return new ThothClass(functions);
    }

    /**
     * Parses the given code and transform it into simple instructions
     * @param code
     *          The code to translate
     * @param params
     *          The names of the different parameters available
     * @return
     *          A list containing all the instructions created from the code
     * @throws ThothParserException
     *          Thrown if any error happened while parsing
     */
    private List<ThothCommandment> interpret(String code, String[] params) throws ThothParserException {
        nodeID = 0; // Reset the nodes
        nodeBase = "N"; // ditto
        boolean inCode = false;
        StringBuffer currentText = new StringBuffer();
        List<ThothCommandment> instructionList = new ArrayList<>();
        instructionList.add(newLabelNode()); // We add the LABEL N0 instruction, all functions start that way
        Map<String, Integer> paramMap = new HashMap<>();
        for(int i = 0;i<params.length;i++) { // Create a map param name to param index
            paramMap.put(params[i], i);
        }
        for(int j = 0;j<code.length();j++) {
            char c = code.charAt(j);
            if(c == ' ') {
                // TODO: Correct spaces
                currentText.append(c);
            } else if(c == '\n') {
                // NOOP
                // TODO: Add a way to break lines
            } else if(c == '|') {
                inCode = !inCode;
                if(!inCode) { // We're no longer in code, we can compile it into instructions
                    newNode();
                    instructionList.add(newLabelNode());
                    String scriptCode = currentText.toString();
                    createInstructions(instructionList, paramMap, scriptCode);
                } else {
                    instructionList.add(new TextInstruction(currentText.toString())); // We have raw text, directly output it
                }
                currentText.delete(0, currentText.length()); // empty the buffer
            } else {
                currentText.append(c);
            }
        }
        if(currentText.length() != 0) {
            instructionList.add(new TextInstruction(currentText.toString())); // We still have text in the buffer, output it
        }
        return instructionList;
    }

    /**
     * Creates instructions from *embedded* code inside the translations
     * @param instructions
     *          The list in which we should add the newly created instructions
     * @param params
     *          The map given param ids from their name
     * @param code
     *          The embedded code
     * @throws ThothParserException
     *          Thrown if any error happened while parsing the embedded code
     */
    private void createInstructions(List<ThothCommandment> instructions, Map<String, Integer> params, String code) throws ThothParserException {
        StringBuffer buffer = new StringBuffer();
        char[] chars = code.toCharArray();
        boolean gettingField = false; // boolean checking if we are accessing a field of a param by using the '->' operator
        int column = 0;
        int line = 1;
        String param; // The current parameter's name
        for(int i = 0;i<chars.length;i++) {
            char c = chars[i];
            boolean escaped = false;
            if(c == '\n') {
                line++;
                column = 0;
            }
            if(i != 0) {
                escaped = chars[i-1] == '\\';
            }
            char next = '\0';
            if(i < chars.length-1) {
                next = chars[i+1];
            }
            if(escaped) {
                if(c == 'n') {
                    buffer.append('\n');
                } else {
                    buffer.append(c);
                }
            } else if(c == '-') {
                if(next == '>') { // we are accessing a param field with the '->' operator
                    if(!gettingField) {
                        gettingField = true;
                        i++; // skip the '>' symbol
                        param = buffer.toString();
                        buffer.delete(0, buffer.length());
                        if(param.isEmpty()) {
                            throwParserException("Cannot get a field from nothing!", i, column, line);
                        }
                        if(!params.containsKey(param)) {
                            throwParserException("Invalid variable name: "+param, i, column, line);
                        } else {
                            int index = params.get(param);
                            instructions.add(new ParamInstruction(index));
                        }
                    } else {
                        throwParserException("Nested fields are not surpported", i, column, line);
                    }
                }
            } else if(c == ' ') {
                String s = buffer.toString();
                if(gettingField) {
                    instructions.add(new FlagInstruction(s));
                    instructions.add(new PopInstruction());
                    // Pop the flag value
                } else {
                    if(!s.isEmpty()) {
                        if (!params.containsKey(s)) {
                            throwParserException("Invalid variable name: " + s, i, column, line);
                        } else {
                            int index = params.get(s);
                            instructions.add(new ParamInstruction(index));
                            instructions.add(new PopInstruction());
                            // Pop the param content
                        }
                    }
                }
                buffer.delete(0, buffer.length());
                param = null;
            // TODO: '~' or '!?' -> reverses condition
            } else if(c == '?') {
                String s = buffer.toString();
                if(gettingField) {
                    instructions.add(new FlagInstruction(s));
                } else {
                    if(!s.isEmpty()) {
                        if (!params.containsKey(s)) {
                            throwParserException("Invalid variable name: " + s, i, column, line);
                        } else {
                            int index = params.get(s);
                            instructions.add(new ParamInstruction(index));
                        }
                    }
                }
                String dest = newNode();
                rollbackNode();
                instructions.add(new PushCondition());
                instructions.add(new JumpNotTrueInsn(dest)); // Jumps to next label in the same level if the condition is not true
                param = null;
                buffer.delete(0, buffer.length());
            } else if(c == '{') {
                pushNode();
                instructions.add(newLabelNode()); // Makes the current node level increase
            } else if(c == '}') {
                popNode();
                newNode();
                instructions.add(newLabelNode()); // Makes the current node level decrease
            }
            else {
                buffer.append(c);
            }

            column++;
        }
        if(buffer.length() != 0) { // Empty the buffer if needed
            String s = buffer.toString();
            if(gettingField) {
                instructions.add(new FlagInstruction(s));
                System.out.println("Flags: "+s);
                instructions.add(new PopInstruction());
            } else {
                if(!s.isEmpty()) {
                    if (!params.containsKey(s)) {
                        throwParserException("Invalid variable name: " + s, chars.length-1, column, line);
                    } else {
                        int index = params.get(s);
                        instructions.add(new ParamInstruction(index));
                        instructions.add(new PopInstruction());
                    }
                }
            }
        }
    }

    /**
     * Throws an exception with a formatted error message
     * @param message
     *          The reason of the exception
     * @param index
     *          The character index of the problem
     * @param column
     *          The column number of the problem
     * @param line
     *          The line number of the problem
     * @throws ThothParserException
     *          The newly created exception
     */
    private void throwParserException(String message, int index, int column, int line) throws ThothParserException {
        throw new ThothParserException(message+" at index "+index+", column "+column+", line "+line);
    }

    private ThothCommandment newLabelNode() {
        return new LabelInstruction(nodeBase+nodeID);
    }

    private void rollbackNode() {
        nodeID--;
    }

    private String newNode() {
        nodeID++;
        return nodeBase+nodeID;
    }

    /**
     * Makes the node level decrease
     */
    private void popNode() {
        nodeBase = nodeBase.substring(0, nodeBase.lastIndexOf("-"));
        if(nodeBase.contains("-"))
            nodeID = Integer.parseInt(nodeBase.substring(nodeBase.lastIndexOf("-")+1, nodeBase.length()));
        else
            nodeID = Integer.parseInt(nodeBase.substring(1, nodeBase.length()));
        nodeBase = nodeBase.substring(0, nodeBase.length()-String.valueOf(nodeID).length()); // Removes the last nodeID
    }

    /**
     * Makes the node level increase
     */
    private void pushNode() {
        nodeBase = nodeBase+nodeID+"-";
        nodeID = 0;
    }

    /**
     * Creates a type flag from the function definition
     * @param rawLang
     *          The raw content of the file
     * @param equalSign
     *          The index of the '=' operator of the current function
     * @param typeDef
     *          The index of the ':' operator of the current function or -1 if none
     * @return
     *          The flag type based on the type given in the function definition
     */
    private int handleType(String rawLang, int equalSign, int typeDef) {
        int typeFlag = 0;
        if(dneois(equalSign, typeDef)) { // No type def
            typeFlag = FLAG_NEUTRAL;
        } else {
            String type = rawLang.substring(typeDef+1, equalSign);
            for(int j = 0;j<type.length();j++) {
                char c = type.charAt(j);
                if(idsToFlag.containsKey(c)) {
                    int flag = idsToFlag.get(c);
                    typeFlag |= flag;
                }
            }
            if(typeFlag == 0) {
                typeFlag = FLAG_NEUTRAL;
            }
        }
        return typeFlag;
    }

    /**
     * Does Not Exists Or Is After
     * @return
     *      Returns true if the arg is smaller than the limit OR that the limit is negative
     */
    private boolean dneois(int arg, int limit) {
        if(arg < limit)
            return true;
        return limit < 0;
    }

    /**
     * Gives the index of the first occurrence of <code>toSeek</code> from the <code>currentIndex</code>
     * @param data
     *      The string to search in
     * @param currentIndex
     *      The index to search from
     * @param toSeek
     *      The string to seek to
     * @return
     *      The index of <code>toSeek</code> or -1
     */
    private int seek(String data, int currentIndex, String toSeek) {
        return data.indexOf(toSeek, currentIndex);
    }
}
