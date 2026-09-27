import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Parser implements AutoCloseable {
    private BufferedReader reader;
    private String currentLine;
    private int lineNumber = 0;

    public enum InstructionType { A_INSTRUCTION, C_INSTRUCTION, L_INSTRUCTION }

    public Parser(String fileName) throws IOException {
        reader = new BufferedReader(new FileReader(fileName));
        advance();
    }

    public boolean hasMoreLines() {
        return currentLine != null;
    }

    public int lineNumber() {
        return lineNumber;  // line of the current instruction in the source file
    }

    private String removeCommentsAndWhitespace(String line) {
        if (line == null) {
            return null;
        }
        line = line.replaceAll("\\s+", "");
        int commentIndex = line.indexOf("//");
        if (commentIndex != -1) {
            return line.substring(0, commentIndex);
        } else {
            return line;
        }
    }

    public void advance() throws IOException {
        do {
            currentLine = reader.readLine();
            if (currentLine != null) {
                lineNumber++;
                currentLine = removeCommentsAndWhitespace(currentLine);
            }
        } while (currentLine != null && currentLine.isEmpty());  // try again if the line is empty
    }

    public InstructionType instructionType() {
        if (currentLine.startsWith("@")) {
            return InstructionType.A_INSTRUCTION;
        } else if (currentLine.startsWith("(")) {
            return InstructionType.L_INSTRUCTION;
        } else {
            return InstructionType.C_INSTRUCTION;
        }
    }

    public String symbol() {
        if (instructionType() == InstructionType.L_INSTRUCTION) {
            return currentLine.substring(1, currentLine.length() - 1);  // part inside parentheses
        } else if (instructionType() == InstructionType.A_INSTRUCTION) {
            return currentLine.substring(1);  // part after `@`
        }
        return "";
    }

    public String dest() {
        if (instructionType() != InstructionType.C_INSTRUCTION) {
            throw new IllegalStateException("dest() can only be called on C-instructions!");
        }
        int equalsIndex = currentLine.indexOf("=");
        return equalsIndex != -1 ? currentLine.substring(0, equalsIndex) : null;
    }

    public String comp() {
        if (instructionType() != InstructionType.C_INSTRUCTION) {
            throw new IllegalStateException("comp() can only be called on C-instructions!");
        }
        int equalsIndex = currentLine.indexOf("=");
        int semicolonIndex = currentLine.indexOf(";");

        if (equalsIndex != -1 && semicolonIndex != -1) {
            return currentLine.substring(equalsIndex + 1, semicolonIndex);
        } else if (equalsIndex != -1) {
            return currentLine.substring(equalsIndex + 1);
        } else if (semicolonIndex != -1) {
            return currentLine.substring(0, semicolonIndex);
        } else {
            return currentLine;
        }
    }

    public String jump() {
        if (instructionType() != InstructionType.C_INSTRUCTION) {
            throw new IllegalStateException("jump() can only be called on C-instructions!");
        }
        int semicolonIndex = currentLine.indexOf(";");
        return semicolonIndex != -1 ? currentLine.substring(semicolonIndex + 1) : null;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}