import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

public class CompilationEngine {
    private VMWriter writer;
    private JackTokenizer tokenizer;
    private SymbolTable symbolTable;
    private String className;
    private String currentFunction;
    private int whileCounter;  // necessary for the labels
    private int ifCounter;  // necessary for the labels

    public CompilationEngine(JackTokenizer tokenizer, String outputPath) throws Exception {
        this.writer = new VMWriter(outputPath);
        this.tokenizer = tokenizer;
        this.symbolTable = new SymbolTable();
    }

    public void compileClass() throws Exception {
        consumeKeyword("class");
        className = tokenizer.identifier();
        consumeIdentifier();  // consume class name
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
        writer.close();
    }

    public void compileClassVarDec() throws Exception {
        SymbolTable.Kind kind;
        if (tokenizer.keyword() == KeywordType.STATIC) {
            kind = SymbolTable.Kind.STATIC;
        } else {
            kind = SymbolTable.Kind.FIELD;
        }

        String type;
        if (tokenizer.tokenType() == TokenType.KEYWORD) {
            type = tokenizer.keyword().toString().toLowerCase();
        } else {  // it's an object, type = class name
            type = tokenizer.identifier();
        }
        compileType();

        // add new variable to symbol table:
        String name = tokenizer.identifier();
        symbolTable.define(name, type, kind);
        consumeIdentifier();

        while (tokenizer.symbol() == ',') {
            consumeSymbol(',');
            name = tokenizer.identifier();
            symbolTable.define(name, type, kind);
            consumeIdentifier();
        }
        consumeSymbol(';');
    }

    public void compileSubroutine() throws Exception {
        String subroutineType = tokenizer.keyword().toString().toLowerCase();
        if (subroutineType.equals("method")) {
            symbolTable.define("this", className, SymbolTable.Kind.ARG);
        }
        consumeKeyword("constructor", "function", "method");


        String returnType;
        if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == KeywordType.VOID) {
            returnType = "void";
            consumeKeyword("void");
        } else {
            returnType = tokenizer.keyword().toString().toLowerCase();
            compileType();
        }

        String functionName = tokenizer.identifier();
        consumeIdentifier();
        currentFunction = className + "." + functionName;

        consumeSymbol('(');
        compileParameterList();
        consumeSymbol(')');
        compileSubroutineBody(subroutineType);
    }

    public void compileParameterList() throws Exception {
        // ((type varName) (, type varName)*)?
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ')') {
            String type;
            if (tokenizer.tokenType() == TokenType.KEYWORD) {
                type = tokenizer.keyword().toString().toLowerCase();
            } else {
                type = tokenizer.identifier();
            }
            compileType();

            String name = tokenizer.identifier();
            symbolTable.define(name, type, SymbolTable.Kind.ARG);
            consumeIdentifier();

            // there are more parameters:
            while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
                consumeSymbol(',');
                if (tokenizer.tokenType() == TokenType.KEYWORD) {
                    type = tokenizer.keyword().toString().toLowerCase();
                } else {
                    type = tokenizer.identifier();
                }
                compileType();
                name = tokenizer.identifier();
                symbolTable.define(name, type, SymbolTable.Kind.ARG);
                consumeIdentifier();
            }
        }
    }

    public void compileSubroutineBody(String subroutineType) throws Exception {
        consumeSymbol('{');
        int nVars = 0;
        while (tokenizer.tokenType() == TokenType.KEYWORD &&
                tokenizer.keyword() == KeywordType.VAR) {
            nVars += compileVarDec();
        }

        writer.writeFunction(currentFunction, nVars);

        if (subroutineType.equals("constructor")) {
            // Allocate necessary memory for the object:
            int fieldCount = symbolTable.varCount(SymbolTable.Kind.FIELD);
            writer.writePush(VMWriter.Segment.CONST, fieldCount);
            writer.writeCall("Memory.alloc", 1);
            writer.writePop(VMWriter.Segment.POINTER, 0);
        } else if (subroutineType.equals("method")) {
            // Set THIS to 0:
            writer.writePush(VMWriter.Segment.ARG, 0);
            writer.writePop(VMWriter.Segment.POINTER, 0);
        }

        compileStatements();
        consumeSymbol('}');
    }

    private int compileVarDec() throws Exception {
        // var type varName (, varName)* ;
        int varCount = 0;
        consumeKeyword("var");
        
        String type = tokenizer.tokenType() == TokenType.KEYWORD ? 
            tokenizer.keyword().toString().toLowerCase() : tokenizer.identifier();
        compileType();

        String name = tokenizer.identifier();
        symbolTable.define(name, type, SymbolTable.Kind.VAR);
        consumeIdentifier();
        varCount++;

        while (tokenizer.symbol() == ',') {
            consumeSymbol(',');
            name = tokenizer.identifier();
            symbolTable.define(name, type, SymbolTable.Kind.VAR);
            consumeIdentifier();
            varCount++;
        }
        consumeSymbol(';');
        return varCount;
    }

    private void compileLet() throws Exception {
        // let varName ([expression])? = expression ;
        consumeKeyword("let");
        
        String varName = tokenizer.identifier();
        consumeIdentifier();

        // Handle array assignment
        boolean isArray = false;
        if (tokenizer.symbol() == '[') {
            isArray = true;
            consumeSymbol('[');
            compileExpression();  // Push array index
            consumeSymbol(']');
            
            // Push array base address
            SymbolTable.Kind kind = symbolTable.kindOf(varName);
            int index = symbolTable.indexOf(varName);
            writer.writePush(kindToSegment(kind), index);
            
            // Calculate target address
            writer.writeArithmetic(VMWriter.Command.ADD);
        }

        consumeSymbol('=');
        compileExpression();  // Push value to assign
        consumeSymbol(';');

        if (isArray) {
            writer.writePop(VMWriter.Segment.TEMP, 0);     
            writer.writePop(VMWriter.Segment.POINTER, 1);  
            writer.writePush(VMWriter.Segment.TEMP, 0);    
            writer.writePop(VMWriter.Segment.THAT, 0);     
        } else {
            SymbolTable.Kind kind = symbolTable.kindOf(varName);
            int index = symbolTable.indexOf(varName);
            writer.writePop(kindToSegment(kind), index);
        }
    }

    private void compileIf() throws Exception {
        String labelTrue = "IF_TRUE" + ifCounter;
        String labelFalse = "IF_FALSE" + ifCounter;
        String labelEnd = "IF_END" + ifCounter;
        ifCounter++;

        consumeKeyword("if");
        consumeSymbol('(');
        compileExpression();
        consumeSymbol(')');

        writer.writeIf(labelTrue);
        writer.writeGoto(labelFalse);
        writer.writeLabel(labelTrue);

        consumeSymbol('{');
        compileStatements();
        consumeSymbol('}');

        if (tokenizer.tokenType() == TokenType.KEYWORD && 
            tokenizer.keyword() == KeywordType.ELSE) {
            writer.writeGoto(labelEnd);
            writer.writeLabel(labelFalse);
            consumeKeyword("else");
            consumeSymbol('{');
            compileStatements();
            consumeSymbol('}');
            writer.writeLabel(labelEnd);
        } else {
            writer.writeLabel(labelFalse);
        }
    }

    private void compileWhile() throws Exception {
        String labelLoop = "WHILE_EXP" + whileCounter;
        String labelEnd = "WHILE_END" + whileCounter;
        whileCounter++;

        consumeKeyword("while");
        writer.writeLabel(labelLoop);
        
        consumeSymbol('(');
        compileExpression();
        consumeSymbol(')');

        writer.writeArithmetic(VMWriter.Command.NOT);
        writer.writeIf(labelEnd);

        consumeSymbol('{');
        compileStatements();
        consumeSymbol('}');

        writer.writeGoto(labelLoop);
        writer.writeLabel(labelEnd);
    }

    private void compileDo() throws Exception {
        consumeKeyword("do");
        compileSubroutineCall();
        consumeSymbol(';');
        // Void methods/functions must pop the returned value
        writer.writePop(VMWriter.Segment.TEMP, 0);
    }

    private void compileReturn() throws Exception {
        consumeKeyword("return");
        if (tokenizer.tokenType() != TokenType.SYMBOL || 
            tokenizer.symbol() != ';') {
            compileExpression();
        } else {
            writer.writePush(VMWriter.Segment.CONST, 0);  // Push 0 for void functions
        }
        consumeSymbol(';');
        writer.writeReturn();
    }

    private void compileExpression() throws Exception {
        compileTerm();
        while (isOp()) {
            char op = tokenizer.symbol();
            consumeSymbol(op);
            compileTerm();
            compileOp(op);
        }
    }

    private void compileTerm() throws Exception {
        switch (tokenizer.tokenType()) {
            case INT_CONST:
                writer.writePush(VMWriter.Segment.CONST, tokenizer.intVal());
                consumeIntegerConstant();
                break;

            case STRING_CONST:
                String str = tokenizer.stringVal();
                writer.writePush(VMWriter.Segment.CONST, str.length());
                writer.writeCall("String.new", 1);
                for (char c : str.toCharArray()) {
                    writer.writePush(VMWriter.Segment.CONST, (int)c);
                    writer.writeCall("String.appendChar", 2);
                }
                consumeStringConstant();
                break;

            case KEYWORD:
                if (isKeywordConstant()) {
                    KeywordType keyword = tokenizer.keyword();
                    switch (keyword) {
                        case TRUE:
                            writer.writePush(VMWriter.Segment.CONST, 0);
                            writer.writeArithmetic(VMWriter.Command.NOT);
                            break;
                        case FALSE:
                        case NULL:
                            writer.writePush(VMWriter.Segment.CONST, 0);
                            break;
                        case THIS:
                            writer.writePush(VMWriter.Segment.POINTER, 0);
                            break;
                    }
                    consumeKeyword("true", "false", "null", "this");
                }
                break;

            case IDENTIFIER:
                String name = tokenizer.identifier();
                tokenizer.advance();
                
                if (tokenizer.tokenType() == TokenType.SYMBOL && 
                    tokenizer.symbol() == '[') {
                    // Array access
                    consumeSymbol('[');
                    compileExpression();
                    consumeSymbol(']');
                    
                    SymbolTable.Kind kind = symbolTable.kindOf(name);
                    int index = symbolTable.indexOf(name);
                    writer.writePush(kindToSegment(kind), index);
                    writer.writeArithmetic(VMWriter.Command.ADD);
                    writer.writePop(VMWriter.Segment.POINTER, 1);
                    writer.writePush(VMWriter.Segment.THAT, 0);
                } else if (tokenizer.tokenType() == TokenType.SYMBOL && 
                         (tokenizer.symbol() == '(' || tokenizer.symbol() == '.')) {
                    // Subroutine call
                    compileSubroutineCall(name);
                } else {
                    // Variable
                    SymbolTable.Kind kind = symbolTable.kindOf(name);
                    int index = symbolTable.indexOf(name);
                    writer.writePush(kindToSegment(kind), index);
                }
                break;

            case SYMBOL:
                if (tokenizer.symbol() == '(') {
                    consumeSymbol('(');
                    compileExpression();
                    consumeSymbol(')');
                } else if (isUnaryOp()) {
                    char op = tokenizer.symbol();
                    consumeSymbol(op);
                    compileTerm();
                    if (op == '-') {
                        writer.writeArithmetic(VMWriter.Command.NEG);
                    } else if (op == '~') {
                        writer.writeArithmetic(VMWriter.Command.NOT);
                    }
                }
                break;
        }
    }

    private void compileSubroutineCall() throws Exception {
        String name = tokenizer.identifier();
        compileSubroutineCall(name);
    }

    private void compileSubroutineCall(String name) throws Exception {
        int nArgs = 0;
        String functionName;

        if (tokenizer.symbol() == '.') {
            consumeSymbol('.');
            String methodName = tokenizer.identifier();
            consumeIdentifier();

            // Check if it's a method call on an object
            SymbolTable.Kind kind = symbolTable.kindOf(name);
            if (kind != null) {
                String type = symbolTable.typeOf(name);
                int index = symbolTable.indexOf(name);
                writer.writePush(kindToSegment(kind), index);
                functionName = type + "." + methodName;
                nArgs = 1;
            } else {
                // It's a function call
                functionName = name + "." + methodName;
            }
        } else {
            // Method call on this object
            writer.writePush(VMWriter.Segment.POINTER, 0);
            functionName = className + "." + name;
            nArgs = 1;
        }

        consumeSymbol('(');
        nArgs += compileExpressionList();
        consumeSymbol(')');

        writer.writeCall(functionName, nArgs);
    }

    private int compileExpressionList() throws Exception {
        int nArgs = 0;
        if (tokenizer.tokenType() != TokenType.SYMBOL || 
            tokenizer.symbol() != ')') {
            compileExpression();
            nArgs = 1;
            
            while (tokenizer.tokenType() == TokenType.SYMBOL && 
                   tokenizer.symbol() == ',') {
                consumeSymbol(',');
                compileExpression();
                nArgs++;
            }
        }
        return nArgs;
    }

    private void compileStatements() throws Exception {
        while (isStatementKeyword()) {
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
    }

    private void compileOp(char op) throws IOException {
        switch (op) {
            case '+':
                writer.writeArithmetic(VMWriter.Command.ADD);
                break;
            case '-':
                writer.writeArithmetic(VMWriter.Command.SUB);
                break;
            case '*':
                writer.writeCall("Math.multiply", 2);
                break;
            case '/':
                writer.writeCall("Math.divide", 2);
                break;
            case '&':
                writer.writeArithmetic(VMWriter.Command.AND);
                break;
            case '|':
                writer.writeArithmetic(VMWriter.Command.OR);
                break;
            case '<':
                writer.writeArithmetic(VMWriter.Command.LT);
                break;
            case '>':
                writer.writeArithmetic(VMWriter.Command.GT);
                break;
            case '=':
                writer.writeArithmetic(VMWriter.Command.EQ);
                break;
        }
    }

    private boolean isOp() {
        return tokenizer.tokenType() == TokenType.SYMBOL && 
               "+-*/&|<>=".indexOf(tokenizer.symbol()) != -1;
    }

    private boolean isUnaryOp() {
        return tokenizer.tokenType() == TokenType.SYMBOL && 
               "-~".indexOf(tokenizer.symbol()) != -1;
    }

    private boolean isKeywordConstant() {
        return tokenizer.tokenType() == TokenType.KEYWORD && 
               (tokenizer.keyword() == KeywordType.TRUE ||
                tokenizer.keyword() == KeywordType.FALSE ||
                tokenizer.keyword() == KeywordType.NULL ||
                tokenizer.keyword() == KeywordType.THIS);
    }

    private boolean isStatementKeyword() {
        return tokenizer.tokenType() == TokenType.KEYWORD &&
               (tokenizer.keyword() == KeywordType.LET ||
                tokenizer.keyword() == KeywordType.IF ||
                tokenizer.keyword() == KeywordType.WHILE ||
                tokenizer.keyword() == KeywordType.DO ||
                tokenizer.keyword() == KeywordType.RETURN);
    }

    private VMWriter.Segment kindToSegment(SymbolTable.Kind kind) {
        switch (kind) {
            case STATIC: return VMWriter.Segment.STATIC;
            case FIELD: return VMWriter.Segment.THIS;
            case ARG: return VMWriter.Segment.ARG;
            case VAR: return VMWriter.Segment.LOCAL;
            default: throw new IllegalArgumentException("Unknown kind: " + kind);
        }
    }

    // Helper methods for consuming tokens
    private void consumeIdentifier() throws Exception {
        if (tokenizer.tokenType() != TokenType.IDENTIFIER) {
            throw new Exception("Expected identifier, but found " + tokenizer.tokenType());
        }
        tokenizer.advance();
    }

    private void consumeSymbol(char symbol) throws Exception {
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != symbol) {
            throw new Exception("Expected symbol " + symbol + ", but found " + 
                              tokenizer.tokenType() + ": " + tokenizer.symbol());
        }
        tokenizer.advance();
    }

    private void consumeKeyword(String... keywords) throws Exception {
        if (tokenizer.tokenType() != TokenType.KEYWORD) {
            throw new Exception("Expected keyword, but found " + tokenizer.tokenType());
        }
        boolean found = false;
        for (String keyword : keywords) {
            if (tokenizer.keyword() == KeywordType.valueOf(keyword.toUpperCase())) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new Exception("Expected one of " + String.join(", ", keywords) + 
                              ", but found " + tokenizer.keyword());
        }
        tokenizer.advance();
    }

    private void consumeIntegerConstant() throws Exception {
        if (tokenizer.tokenType() != TokenType.INT_CONST) {
            throw new Exception("Expected integer constant, but found " + tokenizer.tokenType());
        }
        tokenizer.advance();
    }

    private void consumeStringConstant() throws Exception {
        if (tokenizer.tokenType() != TokenType.STRING_CONST) {
            throw new Exception("Expected string constant, but found " + tokenizer.tokenType());
        }
        tokenizer.advance();
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
            throw new Exception("Expected type, but found " + tokenizer.tokenType());
        }
    }
}