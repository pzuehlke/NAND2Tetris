import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class Parser {
    private BufferedReader reader;
    private String[] tokens;

    private static HashMap<String, String> typeMap = new HashMap<>();
    static {
        typeMap.put("push",     "C_PUSH");
        typeMap.put("pop",      "C_POP");
        typeMap.put("label",    "C_LABEL");
        typeMap.put("goto",     "C_GOTO");
        typeMap.put("if-goto",  "C_IF");
        typeMap.put("function", "C_FUNCTION");
        typeMap.put("return",   "C_RETURN");
        typeMap.put("call",     "C_CALL");
        for (String op : new String[] {"add", "sub", "neg", "eq", "gt", "lt", "and", "or", "not"}) {
            typeMap.put(op, "C_ARITHMETIC");
        }
    }

    public Parser(String filePath) throws IOException {
        reader = new BufferedReader(new FileReader(filePath));
        advance();
    }

    public boolean hasMoreLines() {
        return tokens != null;
    }

    public void advance() throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            // Strip comments and whitespace, then skip the line if nothing is left:
            int commentIndex = line.indexOf("//");
            if (commentIndex != -1) {
                line = line.substring(0, commentIndex);
            }
            line = line.trim();
            if (!line.isEmpty()) {
                tokens = line.split("\\s+");
                return;
            } // else, the line is empty, go for another iteration
        }
        tokens = null;  // end of file
    }

    public String commandType() {
        return typeMap.getOrDefault(tokens[0], "C_UNKNOWN");
    }

    public String command() {   // not in the API
        return tokens[0];
    }

    public String arg1() {
        if (commandType().equals("C_ARITHMETIC")) {
            return tokens[0];   // arithmetic commands have no arguments
        } else {
            return tokens[1];
        }
    }

    public int arg2() {
        String type = commandType();
        if (type.equals("C_PUSH")     || type.equals("C_POP")   ||
            type.equals("C_FUNCTION") || type.equals("C_CALL")) {
            return Integer.parseInt(tokens[2]);
        } else {
            throw new IllegalArgumentException("arg2 not valid for this command "
                + "type: " + type);
        }
    }

    public void close() throws IOException {
        reader.close();
    }
}