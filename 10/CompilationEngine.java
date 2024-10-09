import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

public class CompilationEngine {
    private BufferedWriter writer;
    private JackTokenizer tokenizer;
    private int indentationLevel;

    public CompilationEngine(JackTokenizer tokenizer, String outputPath) throws Exception {
        this.writer = new BufferedWriter(new FileWriter(outputPath));
        this.tokenizer = tokenizer;
        this.indentationLevel = 0;
    }

    public void compileClass() throws Exception {
        // The text says we should add a "tokens" tag, but comparison fails if we add it:
        // beginTag("tokens"); 
        beginTag("class");
        consumeKeyword("class");
        consumeIdentifier();
        consumeSymbol('{');

        // Compile class variable declarations:
        while (tokenizer.tokenType() == TokenType.KEYWORD &&
                (tokenizer.keyword() == KeywordType.STATIC || tokenizer.keyword() == KeywordType.FIELD)) {
            compileClassVarDec();
        }

        // Compile subroutine definitions:
        while (tokenizer.tokenType() == TokenType.KEYWORD &&
                (tokenizer.keyword() == KeywordType.CONSTRUCTOR ||
                        tokenizer.keyword() == KeywordType.FUNCTION ||
                        tokenizer.keyword() == KeywordType.METHOD)) {
            compileSubroutine();
        }

        consumeSymbol('}');
        closeTag("class");
        // The text says we should add a "tokens" tag, but comparison fails if we add it:
        // closeTag("tokens");
        writer.close();
    }

    public void compileClassVarDec() throws Exception {
        beginTag("classVarDec");
        consumeKeyword("static", "field");
        compileType();
        consumeIdentifier();
        while (tokenizer.symbol() == ',') {
            consumeSymbol(',');
            consumeIdentifier();
        }
        consumeSymbol(';');
        closeTag("classVarDec");
    }

    public void compileSubroutine() throws Exception {
        beginTag("subroutineDec");
        consumeKeyword("constructor", "function", "method");
        if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == KeywordType.VOID) {
            consumeKeyword("void");
        } else {
            compileType();
        }
        consumeIdentifier();
        consumeSymbol('(');
        compileParameterList();
        consumeSymbol(')');
        compileSubroutineBody();
        closeTag("subroutineDec");
    }

    public void compileParameterList() throws Exception {
        beginTag("parameterList");
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ')') {
            compileType();
            consumeIdentifier();
            while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
                consumeSymbol(',');
                compileType();
                consumeIdentifier();
            }
        }
        closeTag("parameterList");
    }

    public void compileSubroutineBody() throws Exception {
        beginTag("subroutineBody");
        consumeSymbol('{');
        while (tokenizer.tokenType() == TokenType.KEYWORD &&
                tokenizer.keyword() == KeywordType.VAR) {
            compileVarDec();
        }
        compileStatements();
        consumeSymbol('}');
        closeTag("subroutineBody");
    }

    public void compileVarDec() throws Exception {
        beginTag("varDec");
        consumeKeyword("var");
        compileType();
        consumeIdentifier();
        while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            consumeSymbol(',');
            consumeIdentifier();
        }
        consumeSymbol(';');
        closeTag("varDec");
    }

    public void compileStatements() throws Exception {
        beginTag("statements");
        while (isStatementKeyword()) {
            compileStatement();
        }
        closeTag("statements");
    }

    private void compileStatement() throws Exception {
        switch (tokenizer.keyword()) {
            case LET:
                compileLet();
                break;
            case IF:
                compileIf();
                break;
            case WHILE:
                compileWhile();
                break;
            case DO:
                compileDo();
                break;
            case RETURN:
                compileReturn();
                break;
            default:
                throw new Exception("Unexpected statement keyword: " + tokenizer.keyword());
        }
    }

    public void compileLet() throws Exception {
        beginTag("letStatement");
        consumeKeyword("let");
        consumeIdentifier();
        if (tokenizer.symbol() == '[') {
            consumeSymbol('[');
            compileExpression();
            consumeSymbol(']');
        }
        consumeSymbol('=');
        compileExpression();
        consumeSymbol(';');
        closeTag("letStatement");
    }

    public void compileIf() throws Exception {
        beginTag("ifStatement");
        consumeKeyword("if");
        consumeSymbol('(');
        compileExpression();
        consumeSymbol(')');
        consumeSymbol('{');
        compileStatements();
        consumeSymbol('}');
        if (tokenizer.tokenType() == TokenType.KEYWORD &&
                tokenizer.keyword() == KeywordType.ELSE) {
            consumeKeyword("else");
            consumeSymbol('{');
            compileStatements();
            consumeSymbol('}');
        }
        closeTag("ifStatement");
    }

    public void compileWhile() throws Exception {
        beginTag("whileStatement");
        consumeKeyword("while");
        consumeSymbol('(');
        compileExpression();
        consumeSymbol(')');
        consumeSymbol('{');
        compileStatements();
        consumeSymbol('}');
        closeTag("whileStatement");
    }

    public void compileDo() throws Exception {
        beginTag("doStatement");
        consumeKeyword("do");
        compileSubroutineCall();
        consumeSymbol(';');
        closeTag("doStatement");
    }

    public void compileReturn() throws Exception {
        beginTag("returnStatement");
        consumeKeyword("return");
        if (tokenizer.tokenType() != TokenType.SYMBOL ||
            tokenizer.symbol() != ';') {
            compileExpression();
        }
        consumeSymbol(';');
        closeTag("returnStatement");
    }

    public void compileExpression() throws Exception {
        // System.out.println("Compiling expression");
        beginTag("expression");
        compileTerm();
        while (isOp()) {
            compileOp();
            compileTerm();
        }
        closeTag("expression");
    }

    public void compileTerm() throws Exception {
        // System.out.println("Compiling term");
        beginTag("term");
        switch (tokenizer.tokenType()) {
            case INT_CONST:
                consumeIntegerConstant();
                break;
            case STRING_CONST:
                consumeStringConstant();
                break;
            case KEYWORD:
                if (isKeywordConstant()) {
                    compileKeywordConstant();
                } else {
                    throw new Exception("Unexpected keyword in term: " + tokenizer.keyword());
                }
                break;
            case IDENTIFIER:
                String nextToken = tokenizer.peekAhead();
                // System.out.println("Peeking ahead to next token: " + nextToken);
                if (nextToken.equals("[")) {
                    consumeIdentifier();
                    consumeSymbol('[');
                    compileExpression();
                    consumeSymbol(']');
                } else if (nextToken.equals("(") ||  nextToken.equals(".")) {
                    compileSubroutineCall();
                } else {
                    consumeIdentifier();
                }
                break;
            case SYMBOL:
                if (tokenizer.symbol() == '(') {
                    consumeSymbol('(');
                    compileExpression();
                    consumeSymbol(')');
                } else if (isUnaryOp()) {
                    compileUnaryOp();
                    compileTerm();
                } else {
                    throw new Exception("Unexpected symbol in term: " + tokenizer.symbol());
                }
                break;
            default:
                throw new Exception("Unexpected token type in term: " + tokenizer.tokenType());
        }
        closeTag("term");
    }

    private void compileSubroutineCall() throws Exception {
        // System.out.println("Compiling subroutine call");
        consumeIdentifier();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '.') {
            consumeSymbol('.');
            consumeIdentifier();
        }
        consumeSymbol('(');
        compileExpressionList();
        consumeSymbol(')');
    }

    public int compileExpressionList() throws Exception {
        // System.out.println("Compiling expression list");
        beginTag("expressionList");
        int expressionCount = 0;
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ')') {
            compileExpression();
            expressionCount = 1;
            while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
                consumeSymbol(',');
                compileExpression();
                expressionCount++;
            }
        }
        closeTag("expressionList");
        return expressionCount;
    }

    private boolean isSymbol(char... symbols) {
        if (tokenizer.tokenType() != TokenType.SYMBOL) {
            return false;
        }
        for (char symbol : symbols) {
            if (tokenizer.symbol() == symbol) {
                return true;
            }
        }
        return false;
    }

    private boolean isOp() {
        return isSymbol('+', '-', '*', '/', '&', '|', '<', '>', '=');
    }

    private boolean isUnaryOp() {
        return isSymbol('-', '~');
    }

    private void compileOp() throws Exception {
        consumeSymbol('+', '-', '*', '/', '&', '|', '<', '>', '=');
    }

    private void compileUnaryOp() throws Exception {
        consumeSymbol('-', '~');
    }

    private void compileKeywordConstant() throws Exception {
        consumeKeyword("true", "false", "null", "this");
    }

    private boolean isStatementKeyword() {
        return (tokenizer.tokenType() == TokenType.KEYWORD &&
                (tokenizer.keyword() == KeywordType.LET    ||
                 tokenizer.keyword() == KeywordType.IF     ||
                 tokenizer.keyword() == KeywordType.WHILE  ||
                 tokenizer.keyword() == KeywordType.DO     ||
                 tokenizer.keyword() == KeywordType.RETURN ));
    }

    private boolean isKeywordConstant() {
        return (tokenizer.tokenType() == TokenType.KEYWORD  &&
                (tokenizer.keyword() == KeywordType.TRUE   ||
                 tokenizer.keyword() == KeywordType.FALSE  ||
                 tokenizer.keyword() == KeywordType.NULL   ||
                 tokenizer.keyword() == KeywordType.THIS   ));
    }

    private void beginTag(String tag) throws IOException {
        String indentation = "  ".repeat(indentationLevel);
        writer.write(indentation + "<" + tag + ">\n");
        indentationLevel++;
    }

    private void closeTag(String tag) throws IOException {
        indentationLevel--;
        String indentation = "  ".repeat(indentationLevel);
        writer.write(indentation + "</" + tag + ">\n");
    }

    private void writeXML(String tag, String content) throws IOException {
        String indentation = "  ".repeat(indentationLevel);
        writer.write(indentation + "<" + tag + "> " + content + " </" + tag + ">\n");
    }

    private void consumeIdentifier() throws Exception {
        if (tokenizer.tokenType() != TokenType.IDENTIFIER) {
            throw new Exception("Expected identifier, but found a token of type " +
                    tokenizer.tokenType());
        } else {
            // For debugging purposes:
            // System.out.println("identifier: " + tokenizer.identifier());
            writeXML("identifier", tokenizer.identifier());
            tokenizer.advance();
        }
    }

    private void consumeSymbol(char... symbols) throws Exception {
        if (tokenizer.tokenType() != TokenType.SYMBOL) {
            throw new Exception("Expected a symbol, but found a token of type " +
                    tokenizer.tokenType());
        }
        boolean found = false;
        for (char symbol : symbols) {
            if (tokenizer.symbol() == symbol) {
                found = true;
                if ("<>\"&".indexOf(symbol) == -1) {
                    writeXML("symbol", String.valueOf(symbol));
                } else {
                    switch (symbol) {
                        case '<':
                            writeXML("symbol", "&lt;");
                            break;
                        case '>':
                            writeXML("symbol", "&gt;");
                            break;
                        case '"':
                            writeXML("symbol", "&quot;");
                            break;
                        case '&':
                            writeXML("symbol", "&amp;");
                            break;
                    }
                }
                tokenizer.advance();
                return;
            }
        }
        if (!found) {
            throw new Exception("Expected a different symbol from " + tokenizer.symbol());
        }
    }

    private void consumeIntegerConstant() throws IOException {
        writeXML("integerConstant", String.valueOf(tokenizer.intVal()));
        tokenizer.advance();
    }

    private void consumeStringConstant() throws IOException {
        writeXML("stringConstant", tokenizer.stringVal());
        tokenizer.advance();
    }

    private void consumeKeyword(String... keywords) throws Exception {
        if (tokenizer.tokenType() != TokenType.KEYWORD) {
            throw new Exception("Expected a keyword, but found a token of type " +
                    tokenizer.tokenType());
        }
        boolean found = false;
        for (String keyword : keywords) {
            if (tokenizer.keyword() == KeywordType.valueOf(keyword.toUpperCase())) {
                found = true;
                // For debugging purposes:
                // System.out.println("keyword: " + keyword);
                writeXML("keyword", keyword);
                tokenizer.advance();
                return;
            }
        }
        if (!found) {
            throw new Exception("Expected a keyword, but found " + tokenizer.keyword());
        }
    }

    private void compileType() throws Exception {
        if (tokenizer.tokenType() == TokenType.KEYWORD &&
            (tokenizer.keyword() == KeywordType.INT ||
             tokenizer.keyword() == KeywordType.CHAR ||
             tokenizer.keyword() == KeywordType.BOOLEAN)) {
            consumeKeyword("int", "char", "boolean");
        } else if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
            consumeIdentifier();
        } else {
            throw new Exception("Expected a type, but found " + tokenizer.tokenType());
        }
    }
}