import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Parser {
    private BufferedReader reader;
    private String currentLine;

    public Parser(String filePath) throws IOException {
        reader = new BufferedReader(new FileReader(filePath));
        advance();
    }

    public boolean hasMoreLines() {
        return currentLine != null;
    }

    public void advance() throws IOException {
        do {
            currentLine = reader.readLine();
        } while (currentLine != null &&
                 (currentLine.isEmpty() ||
                  currentLine.startsWith("//")));
        if (currentLine != null) {
            int slashIndex = currentLine.indexOf("/");
            if (slashIndex != -1) {
                currentLine = currentLine.substring(0, slashIndex);
            }
            currentLine = currentLine.trim();
            System.out.println(currentLine);
        }
    }

    public String commandType() throws IOException {
        if (currentLine == null) {
            return "C_UNKNOWN";
        }

        else if (currentLine.startsWith("//")) {
            return "C_COMMENT"; }
        else if (currentLine.startsWith("push")) {
            return "C_PUSH"; }
        else if (currentLine.startsWith("pop")) {
            return "C_POP"; }
        else if (currentLine.startsWith("label")) {
            return "C_LABEL"; }
        else if (currentLine.startsWith("goto")) {
            return "C_GOTO"; }
        else if (currentLine.startsWith("if-goto")) {
            return "C_IF"; }
        else if (currentLine.startsWith("function")) {
            return "C_FUNCTION"; }
        else if (currentLine.startsWith("return")) {
            return "C_RETURN"; }
        else if (currentLine.startsWith("call")) {
            return "C_CALL"; }
        else if (currentLine.startsWith("add") || 
                 currentLine.startsWith("sub") ||
                 currentLine.startsWith("neg") ||
                 currentLine.startsWith("and") ||
                 currentLine.startsWith("not") ||
                 currentLine.startsWith("or")  ||
                 currentLine.startsWith("eq")  ||
                 currentLine.startsWith("gt")  ||
                 currentLine.startsWith("lt")) {
            return "C_ARITHMETIC"; }
        else {
            return "C_UNKNOWN"; }
    }

    public String command() {
        String[] tokens = currentLine.split(" ");
        return tokens[0];
    }

    public String arg1() throws IOException {
        String[] tokens = currentLine.split(" ");

        if (commandType().equals("C_ARITHMETIC")) {
            return tokens[0];
        }
        else { return tokens[1]; }
    }

    public int arg2() throws IOException {
        String[] tokens = currentLine.split(" ");
        String type = commandType();

        if (type.equals("C_PUSH")     || type.equals("C_POP")   ||
            type.equals("C_FUNCTION") || type.equals("C_CALL")) {
            return Integer.parseInt(tokens[2]); }
        else {
            throw new IllegalArgumentException("arg2 not valid for this command"
                + "type: " + type); }
    }

    public void close() throws IOException {
        reader.close();
    }
}