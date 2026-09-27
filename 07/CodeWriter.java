import java.io.BufferedWriter;
import java.io.File;
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
        // Static variables are named Xxx.i, where Xxx is the file name
        // without directories or extension:
        String name = new File(fileName).getName();
        int dotIndex = name.lastIndexOf(".");
        this.fileName = (dotIndex == -1) ? name : name.substring(0, dotIndex);
    }

    private static HashMap<String, String> segmentMap = new HashMap<>();
    static {
        segmentMap.put("local",    "LCL");
        segmentMap.put("argument", "ARG");
        segmentMap.put("this",     "THIS");
        segmentMap.put("that",     "THAT");
    }

    private static HashMap<String, String> operationMap = new HashMap<>();
    static {
        operationMap.put("add", "M=D+M");
        operationMap.put("sub", "M=M-D");
        operationMap.put("and", "M=D&M");
        operationMap.put("or",  "M=D|M");
        operationMap.put("neg", "M=-M");
        operationMap.put("not", "M=!M");
    }

    private void popIntoD() throws IOException {
        writer.write("// Pop the top value from the stack into D:\n");
        writer.write("@SP\n");
        writer.write("AM=M-1\n");
        writer.write("D=M\n");
    }

    private void pushFromD() throws IOException {
        writer.write("// Push the value in D onto the stack:\n");
        writer.write("@SP\n");
        writer.write("M=M+1\n");
        writer.write("A=M-1\n");
        writer.write("M=D\n");
    }

    public void writeArithmetic(String command) throws IOException {
        writer.write("// " + command + "\n");
        if (command.equals("neg") || command.equals("not")) {
            // Unary operation: point A at the operand on top of the stack:
            writer.write("@SP\n");
            writer.write("A=M-1\n");
        } else {
            // Binary operation: pop the right operand into D and point A at the left one:
            popIntoD();
            writer.write("A=A-1\n");
        }
        // The result overwrites the (left) operand in place, so SP is already correct:
        if (command.equals("eq") || command.equals("gt") || command.equals("lt")) {
            // Save True (-1), then overwrite it with False (0) if the comparison fails:
            String endLabel = "END_" + uniqueLabels++;
            writer.write("D=M-D\n");
            writer.write("M=-1\n");
            writer.write("@" + endLabel + "\n");
            writer.write("D;J" + command.toUpperCase() + "\n");  // JEQ, JGT or JLT
            writer.write("@SP\n");
            writer.write("A=M-1\n");
            writer.write("M=0\n");
            writer.write("(" + endLabel + ")\n");
        } else {
            writer.write(operationMap.get(command) + "\n");
        }
    }

    public void writePushPop(String command, String segment, int index) throws IOException {
        writer.write("// " + command + " " + segment + " " + index + "\n");
        String address = null;
        if (segment.equals("temp"))    { address = "R" + (5 + index); }  // temp starts at 5
        if (segment.equals("pointer")) { address = (index == 0) ? "THIS" : "THAT"; }
        if (segment.equals("static"))  { address = fileName + "." + index; }

        if (command.equals("push")) {
            if (segment.equals("constant")) {
                writer.write("@" + index + "\n");
                writer.write("D=A\n");
            } else if (address != null) {  // temp, pointer or static
                writer.write("@" + address + "\n");
                writer.write("D=M\n");
            } else {  // local, argument, this, that
                writer.write("@" + segmentMap.get(segment) + "\n");
                writer.write("D=M\n");
                writer.write("@" + index + "\n");
                writer.write("A=D+A\n");
                writer.write("D=M\n");
            }
            pushFromD();
        }

        if (command.equals("pop")) {
            if (address != null) {  // temp, pointer or static
                popIntoD();
                writer.write("@" + address + "\n");
                writer.write("M=D\n");
            } else {
                // Store the target address in R13, pop into D, then write D to that address:
                writer.write("@" + segmentMap.get(segment) + "\n");
                writer.write("D=M\n");
                writer.write("@" + index + "\n");
                writer.write("D=D+A\n");
                writer.write("@R13\n");
                writer.write("M=D\n");
                popIntoD();
                writer.write("@R13\n");
                writer.write("A=M\n");
                writer.write("M=D\n");
            }
        }
    }

    public void writeFinalInfiniteLoop() throws IOException {
        writer.write("// End the program with an infinite loop:\n");
        writer.write("(END)\n");
        writer.write("\t@END\n");
        writer.write("\t0;JMP\n");
    }

    public void close() throws IOException {
        writer.close();
    }
}
