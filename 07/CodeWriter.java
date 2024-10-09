import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class CodeWriter {

    private BufferedWriter writer;
    private int uniqueLabels;
    private String fileName;

    public CodeWriter(String fileName) throws IOException {
        writer = new BufferedWriter(new FileWriter(fileName));
        uniqueLabels = 0;
        this.fileName = fileName;
    }

    private static HashMap<String, String> segmentMap = new HashMap<>();
    static {
        segmentMap.put("local", "LCL");
        segmentMap.put("argument", "ARG");
        segmentMap.put("this", "THIS");
        segmentMap.put("that", "THAT");
        segmentMap.put("temp", "TEMP");
        segmentMap.put("pointer", "SP");
        segmentMap.put("constant", "");
    }

    private void popIntoRegister(String register) throws IOException {
        writer.write("// Pop the top value from the stack into register " + register + ":\n");
        writer.write("@SP\n");
        writer.write("M=M-1\n");
        writer.write("A=M\n");
        writer.write(register + "=M\n");
    }

    private void pushFromRegister(String register) throws IOException {
        writer.write("// Push the value in the register " + register + " onto the stack:\n");
        writer.write("@SP\n");
        writer.write("A=M\n");
        writer.write("M=" + register + "\n");
        writer.write("@SP\n");
        writer.write("M=M+1\n");
    }

    public void writeArithmetic(String command) throws IOException {
        // Binary operations:
        if (command.equals("add") || command.equals("sub")
            || command.equals("and") || command.equals("or")) {
            // Pop the right operand into the D register:
            popIntoRegister("D");
            // Pop the left operand, apply the operation and store the result in D:
            writer.write("// Pop the other operand and perform a binary operation; " +
                "save result in D:\n");
            writer.write("@SP\n");
            writer.write("M=M-1\n");
            writer.write("A=M\n");
            if (command.equals("add")) { writer.write("D=D+M\n"); }
            if (command.equals("sub")) { writer.write("D=M-D\n"); }
            if (command.equals("and")) { writer.write("D=D&M\n"); }
            if (command.equals("or"))  { writer.write("D=D|M\n"); }
            // Push the result from the D register onto the stack:
            pushFromRegister("D");
        }

        // Unary operations:
        if (command.equals("neg") || command.equals("not")) {
            popIntoRegister("D");
            // Apply unary operation (negation):
            writer.write("// Apply unary operation (negation):\n");
            if (command.equals("neg")) { writer.write("D=-D\n"); }
            else if (command.equals("not")) { writer.write("D=!D\n"); }
            pushFromRegister("D");
        }

        // Comparison operations:
        if (command.equals("eq") || command.equals("gt") || command.equals("lt")) {
            // Pop the right operand into the D register:
            popIntoRegister("D");
            // Pop the left operand, compute the difference and store the result in D:
            writer.write("@SP\n");
            writer.write("M=M-1\n");
            writer.write("A=M\n");
            writer.write("D=M-D\n");
            // Generate unique labels to realize the comparison and decide which value to push:
            String trueLabel = "TRUE_" + uniqueLabels;
            String endLabel = "END_" + uniqueLabels;
            uniqueLabels++;
            // Test if comparison evaluates to True based on command:
            if (command.equals("eq")) {
                writer.write("// Test equality:\n");
                writer.write("@" + trueLabel + "\n");
                writer.write("D;JEQ\n");
            }
            else if (command.equals("gt")) {
                writer.write("// Test if 1st operand greater than 2nd operand:\n");
                writer.write("@" + trueLabel + "\n");
                writer.write("D;JGT\n");
            }
            else if (command.equals("lt")) {
                writer.write("// Test if 1st operand less than 2nd operand:\n");
                writer.write("@" + trueLabel + "\n");
                writer.write("D;JLT\n");
            }
            // If not True, set D to 0 (False) and jump to endLabel:
            writer.write("// Initially save False (0) in the D register:\n");
            writer.write("D=0\n");
            writer.write("@" + endLabel + "\n");
            writer.write("0;JMP\n");  // Jump to end to skip setting true result
            // trueLabel: If True, set D to -1 (True):
            writer.write("// Comparison holds, save True (-1) in the D register:\n");
            writer.write("(" + trueLabel + ")\n");
            writer.write("D=-1\n");  // D = true
            // endLabel:
            writer.write("(" + endLabel + ")\n");
            // Push the result onto the stack:
            pushFromRegister("D");
        }
    }

    public void writePushPop(String command, String segment, int index) throws IOException {
        if (command.equals("push")) {
            if (segment.equals("constant")) {
                writer.write("// Push a constant into D:\n");
                writer.write("@" + index + "\n");
                writer.write("D=A\n");
            }
            else if (segment.equals("temp")) {
                writer.write("// Push from temp segment into D:\n");
                writer.write("@" + (5 + index) + "\n");  // temp starts at 5
                writer.write("D=M\n");
            } 
            else if (segment.equals("pointer")) {
                writer.write("// Push from pointer segment (THIS/THAT) into D:\n");
                if (index == 0) {
                    writer.write("@THIS\n");  // pointer 0 refers to THIS
                } else {
                    writer.write("@THAT\n");  // pointer 1 refers to THAT
                }
                writer.write("D=M\n");
            } 
            else if (segment.equals("static")) {
                writer.write("// Prepare to push from the static segment into D:\n");
                writer.write("@" + fileName + "." + index + "\n");
                writer.write("D=M\n");
            }
            else {  // indirect memory access for local, argument, this and that:
                writer.write("// Prepare to push from local, argument, this or that segment into D:\n");
                writer.write("@" + segmentMap.get(segment) + "\n");
                writer.write("D=M\n");
                writer.write("@" + index + "\n");
                writer.write("A=D+A\n");
                writer.write("D=M\n");
            }
            pushFromRegister("D");
        }

        if (command.equals("pop")) {
            writer.write("// Store address of " + segment + " " + index + " in register 13:\n");
            if (segment.equals("temp")) {
                writer.write("@" + (5 + index) + "\n");  // temp starts at 5
                writer.write("D=A\n");
            } 
            else if (segment.equals("pointer")) {
                writer.write("// Store address for pointer (THIS/THAT):\n");
                if (index == 0) {
                    writer.write("@THIS\n");  // pointer 0 -> THIS
                } else {
                    writer.write("@THAT\n");  // pointer 1 -> THAT
                }
                writer.write("D=A\n");
            } 
            else if (segment.equals("static")) {
                writer.write("@" + fileName + "." + index + "\n");
                writer.write("D=A\n");
            }
            else {  // indirect memory access for local, argument, this and that:
                writer.write("@" + segmentMap.get(segment) + "\n");
                writer.write("D=M\n");
                writer.write("@" + index + "\n");
                writer.write("D=D+A\n");
            }
            // Store the address temporarily in register 13:
            writer.write("@13\n");
            writer.write("M=D\n");
            popIntoRegister("D");
            writer.write("@13\n");
            writer.write("A=M\n");
            writer.write("M=D\n");
        }
    }

    public void writeFinalInfiniteLoop() throws IOException {
        writer.write("// End the program with an infinite loop:\n");
        writer.write("(END)\n");
        writer.write("@END\n");
        writer.write("0;JMP\n");
    }

    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
}