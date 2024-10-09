import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Parser {
    private BufferedReader reader;
    private String currentLine;

    public enum INSTRUCTION_TYPE { A_INSTRUCTION, C_INSTRUCTION, L_INSTRUCTION };

    public Parser(String fileName) throws IOException {
        reader = new BufferedReader(new FileReader(fileName));
        advance();
    }

    public boolean hasMoreLines() {
        return currentLine != null;
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
                currentLine = removeCommentsAndWhitespace(currentLine);
            }
        } while (currentLine != null && currentLine.isEmpty());
    }

    public INSTRUCTION_TYPE instructionType() {
        if (currentLine.startsWith("@")) {
            return INSTRUCTION_TYPE.A_INSTRUCTION;
        } else if (currentLine.startsWith("(")) {
            return INSTRUCTION_TYPE.L_INSTRUCTION;
        } else {
            return INSTRUCTION_TYPE.C_INSTRUCTION;
        }
    }

    public String symbol() {
        if (instructionType() == INSTRUCTION_TYPE.L_INSTRUCTION) {
            return currentLine.substring(1, currentLine.length() - 1);
        } else if (instructionType() == INSTRUCTION_TYPE.A_INSTRUCTION) {
            return currentLine.substring(1);
        }
        return "";
    }

    public String dest() {
        if (instructionType() != INSTRUCTION_TYPE.C_INSTRUCTION) {
            throw new IllegalStateException("dest() can only be called on C-instructions!");
        }
        int equalsIndex = currentLine.indexOf("=");
        return equalsIndex != -1 ? currentLine.substring(0, equalsIndex) : null;
    }

    public String comp() {
        if (instructionType() != INSTRUCTION_TYPE.C_INSTRUCTION) {
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
        if (instructionType() != INSTRUCTION_TYPE.C_INSTRUCTION) {
            throw new IllegalStateException("jump() can only be called on C-instructions!");
        }
        int semicolonIndex = currentLine.indexOf(";");
        return semicolonIndex != -1 ? currentLine.substring(semicolonIndex + 1) : null;
    }

    public void close() throws IOException {
        reader.close();
    }
}