import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Assembler {

    private static String convertToBinary(int n) {
        StringBuilder result = new StringBuilder();

        while (n > 0) {
            String bit = Integer.toString(n % 2);
            result.append(bit);
            n /= 2;
        }
        result.append("0".repeat(16 - result.length()));
        return result.reverse().toString();
    }

    private static void initializeSymbolTable(Map<String, Integer> symbolTable) {
        for (int i = 0; i <= 15; i++) {
            symbolTable.put("R" + i, i);
        }
        symbolTable.put("SCREEN", 16384);
        symbolTable.put("KBD", 24576);
        symbolTable.put("SP", 0);
        symbolTable.put("LCL", 1);
        symbolTable.put("ARG", 2);
        symbolTable.put("THIS", 3);
        symbolTable.put("THAT", 4);
    }

    public static void main(String[] args) throws IOException {
        File input = new File(args[0]);
        if (input.isFile()) {
            assembleFile(input.getPath());
        } else if (input.isDirectory()) {
            File[] files = input.listFiles((dir, name) -> name.toLowerCase().endsWith(".asm"));
            if (files != null) {
                for (File file : files) {
                    assembleFile(file.getPath());
                }
            }
        } else {
            System.out.println("The provided path is neither a file nor a directory.");
        }
    }

    private static void assembleFile(String assemblyFile) throws IOException {
        String hackFile = assemblyFile.substring(0, assemblyFile.lastIndexOf(".asm")) + ".hack";
        int currentLineNumber = 0;
        int variableCount = 16;
        Map<String, Integer> symbolTable = new HashMap<>();

        initializeSymbolTable(symbolTable);

        // First pass to store the line numbers associated to each label:
        Parser firstPassParser = new Parser(assemblyFile);
        while (firstPassParser.hasMoreLines()) {
            if (firstPassParser.instructionType() == Parser.INSTRUCTION_TYPE.L_INSTRUCTION) {
                symbolTable.put(firstPassParser.symbol(), currentLineNumber);
            } else {
                currentLineNumber++;
            }
            firstPassParser.advance();
        }
        
        // Second pass to actually write the binary code from the assembly program:
        Parser parser = new Parser(assemblyFile);
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(hackFile))) {

            while (parser.hasMoreLines()) {
                Parser.INSTRUCTION_TYPE type = parser.instructionType();
                String code = null;

                if (type == Parser.INSTRUCTION_TYPE.A_INSTRUCTION) {
                    String symbol = parser.symbol();
                    int address;
                    if (symbol.matches("\\d+")) {
                        // non-symbolic address:
                        address = Integer.parseInt(symbol);
                    } else if (!symbolTable.containsKey(symbol)) {
                        // symbol doesn't yet exist in the table, allocate a new address:
                        address = variableCount++;
                        symbolTable.put(symbol, address);
                    } else {
                        address = symbolTable.get(symbol);
                    }
                    code = convertToBinary(address);
                }
                else if (type == Parser.INSTRUCTION_TYPE.C_INSTRUCTION) {
                    code = "111" + Code.comp(parser.comp()) +
                                   Code.dest(parser.dest()) +
                                   Code.jump(parser.jump());
                }

                if (code != null) {
                    writer.write(code);
                    writer.newLine();
                }
                parser.advance();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}