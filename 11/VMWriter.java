import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

public class VMWriter {
    public enum Segment {
        CONST, ARG, LOCAL, STATIC, THIS, THAT, POINTER, TEMP
    }
    
    public enum Command {
        ADD, SUB, NEG, EQ, GT, LT, AND, OR, NOT
    }

    private FileWriter writer;
    private static final Map<Segment, String> segmentMap = new HashMap<>();
    private static final Map<Command, String> commandMap = new HashMap<>();

    static {
        segmentMap.put(Segment.CONST,   "constant");
        segmentMap.put(Segment.ARG,     "argument");
        segmentMap.put(Segment.LOCAL,   "local");
        segmentMap.put(Segment.STATIC,  "static");
        segmentMap.put(Segment.THIS,    "this");
        segmentMap.put(Segment.THAT,    "that");
        segmentMap.put(Segment.POINTER, "pointer");
        segmentMap.put(Segment.TEMP,    "temp");

        commandMap.put(Command.ADD, "add");
        commandMap.put(Command.SUB, "sub");
        commandMap.put(Command.NEG, "neg");
        commandMap.put(Command.EQ,  "eq");
        commandMap.put(Command.GT,  "gt");
        commandMap.put(Command.LT,  "lt");
        commandMap.put(Command.AND, "and");
        commandMap.put(Command.OR,  "or");
        commandMap.put(Command.NOT, "not");
    }

    public VMWriter(String VMFileName) throws IOException {
        this.writer = new FileWriter(VMFileName);
    }

    public void writePush(Segment segment, int index) throws IOException {
        writer.write("push " + segmentMap.get(segment) + " " + index + "\n");
    }

    public void writePop(Segment segment, int index) throws IOException {
        writer.write("pop " + segmentMap.get(segment) + " " + index + "\n");
    }

    public void writeArithmetic(Command command) throws IOException {
        writer.write(commandMap.get(command) + "\n");
    }

    public void writeLabel(String label) throws IOException {
        writer.write("label " + label + "\n");
    }

    public void writeGoto(String label) throws IOException {
        writer.write("goto " + label + "\n");
    }

    public void writeIf(String label) throws IOException {
        writer.write("if-goto " + label + "\n");
    }

    public void writeCall(String name, int nArgs) throws IOException {
        writer.write("call " + name + " " + nArgs + "\n");
    }

    public void writeFunction(String name, int nVars) throws IOException {
        writer.write("function " + name + " " + nVars + "\n");
    }

    public void writeReturn() throws IOException {
        writer.write("return\n");
    }
    
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
}
