import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

public class JackTokenizer {
    private BufferedReader reader;
    private String currentLine;
    private String currentToken;
    private List<String> currentTokens = new ArrayList<>();
    private int currentTokenIndex;

    private static final Set<Character> SYMBOLS = Set.of(
        '{', '}', '(', ')', '[', ']', '.', ',', ';', '+', '-', '*', '/',
        '&', '|', '<', '>', '=', '~'
    );

    private static final Set<String> KEYWORDS = Set.of(
        "class", "constructor", "function", "method", "field", "static",
        "var", "int", "char", "boolean", "void", "true", "false", "null",
        "this", "let", "do", "if", "else", "while", "return"
    );

    private static final Map<String, KeywordType> KEYWORD_MAP = new HashMap<>();
    static {
        for (String keyword : KEYWORDS) {
            KeywordType type = KeywordType.valueOf(keyword.toUpperCase());
            KEYWORD_MAP.put(keyword, type);
        }
    }

    public JackTokenizer(String filePath) throws IOException {
        reader = new BufferedReader(new FileReader(filePath));
        advanceLine();
        advance();
    }

    private boolean hasMoreLines() {
        return (currentLine != null);
    }

    /**
     * Skip over comments, blank lines. Read a new line, extract
     * all tokens in it and save them in `currentTokens`.
     */
    public void advanceLine() throws IOException {
        currentTokens.clear();
        currentTokenIndex = 0;
        StringBuilder token = new StringBuilder();
        boolean inMultiLineComment = false;
        boolean inStringLiteral = false;

        while (true) {
            currentLine = reader.readLine();
            if (currentLine == null) { break; }

            currentLine = removeInlineCommentAndTrim(currentLine);
            if (currentLine.isEmpty()) { continue; }

            // FSM-based implementation. Read one character at a time, decide
            // what to do depending on the current state, which can be:
            // (1) Inside a string literal; (2) Inside a multiline comment; (3) Neither.
            for (int i = 0; i < currentLine.length(); i++) {
                char c = currentLine.charAt(i);

                if (inStringLiteral) {
                    token.append(c);
                    if (c == '"') {
                        inStringLiteral = false;
                        currentTokens.add(token.toString());
                        token.setLength(0);
                    }
                } else if (inMultiLineComment) {
                    if (c == '*' && i < currentLine.length() - 1 &&
                        currentLine.charAt(i + 1) == '/') {
                            i++;
                            inMultiLineComment = false;
                        }
                } else {  // Not in a string nor in a multiline comment:
                    if (c == '"') {
                        if (token.length() > 0) { addTokenAndReset(token); }
                        token.append(c);
                        inStringLiteral = true;
                    } else if (c == '/' && i < currentLine.length() - 1 &&
                               currentLine.charAt(i + 1) == '*') {
                        inMultiLineComment = true;
                        i++;
                    } else if (Character.isWhitespace(c)) {
                        if (token.length() > 0) { addTokenAndReset(token); }
                    } else if (isSymbol(Character.toString(c))) {
                        if (token.length() > 0) { addTokenAndReset(token); }
                        addTokenAndReset(new StringBuilder().append(c));
                    } else {
                        token.append(c);
                    }
                }
            }

            // Reached end of line, hence store the current token:
            if (!inMultiLineComment && !inStringLiteral && token.length() > 0) {
                addTokenAndReset(token);
            }
            // We just read a line, and it contains at least one token:
            if (!inMultiLineComment && !currentTokens.isEmpty()) { break; }
        }
    }

    private void addTokenAndReset(StringBuilder token) {
        currentTokens.add(token.toString());
        // System.out.println("added token: ");  // For debugging purposes
        // System.out.println(token);  // For debugging purposes
        token.setLength(0);
    }

    private String removeInlineCommentAndTrim(String line) {
        int slashIndex = line.indexOf("//");
        return slashIndex != -1 ? (line.substring(0, slashIndex).trim()) : line.trim();
    }

    public boolean hasMoreTokens() {
        return (!currentTokens.isEmpty() || hasMoreLines());
    }

    public void advance() throws IOException {
        if (hasMoreTokens()) {
            if (currentTokenIndex < currentTokens.size()) {
                currentToken = currentTokens.get(currentTokenIndex);
                currentTokenIndex++;
            } else {
                advanceLine();
                if (!currentTokens.isEmpty()) {
                    currentToken = currentTokens.get(0);
                    currentTokenIndex = 1;
                } else {
                    currentToken = null;
                }
            } 
        } else {
            currentToken = null;
        }
    }

    public TokenType tokenType() {
        if (isKeyword(currentToken)) {
            return TokenType.KEYWORD;
        }
        else if (isSymbol(currentToken)) {
            return TokenType.SYMBOL;
        }
        else if (isIntegerConstant(currentToken)) {
            return TokenType.INT_CONST;
        }
        else if (isStringConstant(currentToken)) {
            return TokenType.STRING_CONST;
        }
        else if (isIdentifier(currentToken)) {
            return TokenType.IDENTIFIER;
        }
        else {
            return null;
        }
    }

    public KeywordType keyword() {
        return KEYWORD_MAP.get(currentToken);
    }
    
    public char symbol() {
        return currentToken.charAt(0);
    }

    public String identifier() {
        return currentToken;
    }

    public String peekAhead() throws Exception{
        if (currentTokenIndex < currentTokens.size()) {
                return currentTokens.get(currentTokenIndex);
        } else {
            return null;
        }
    }

    public int intVal() {
        return Integer.parseInt(currentToken);
    }

    public String stringVal() {
        int leftQuotesIndex = currentToken.indexOf("\"");
        int rightQuotesIndex = currentToken.lastIndexOf("\"");
        return currentToken.substring(leftQuotesIndex + 1, rightQuotesIndex);
    }

    private boolean isKeyword(String token) {
        return KEYWORDS.contains(token);
    }

    private boolean isSymbol(String token) {
        return token.length() == 1 && SYMBOLS.contains(token.charAt(0));
    }

    private boolean isStringConstant(String token) {
        int n = token.length();
        return (token.charAt(0) == '"' && token.charAt(n - 1) == '"');
    }

    private boolean isIntegerConstant(String token) {
        try {
            int n = Integer.parseInt(token);
            return 0 <= n && n <= 32767;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidIdentifier(String str) {
        return str.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }

    private boolean isIdentifier(String token) {
        return (!isKeyword(token) && !isSymbol(token) &&
                !isIntegerConstant(token) && !isStringConstant(token) &&
                isValidIdentifier(token));
    }
}