import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Assembler {

    private static String convertToBinary(int n) {
        if (n < 0 || n > 32767) {
            throw new IllegalArgumentException("A-instruction value out of range (0-32767): " + n);
        }
        return String.format("%16s", Integer.toBinaryString(n)).replace(' ', '0');
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

    private static IllegalArgumentException errorAt(String file, int line, String message) {
        return new IllegalArgumentException(file + ":" + line + ": " + message);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: java Assembler <file.asm | directory>");
            return;
        }

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
        // Replace the extension using only the file name, so dots in directory names don't matter:
        File source = new File(assemblyFile);
        String name = source.getName();
        int dotIndex = name.lastIndexOf('.');
        String baseName = dotIndex != -1 ? name.substring(0, dotIndex) : name;
        String hackFile = new File(source.getParentFile(), baseName + ".hack").getPath();

        int romAddress = 0;
        int variableCount = 16;
        Map<String, Integer> symbolTable = new HashMap<>();

        initializeSymbolTable(symbolTable);

        // First pass to store the ROM address associated to each label:
        try (Parser firstPassParser = new Parser(assemblyFile)) {
            while (firstPassParser.hasMoreLines()) {
                if (firstPassParser.instructionType() == Parser.InstructionType.L_INSTRUCTION) {
                    String label = firstPassParser.symbol();
                    if (symbolTable.containsKey(label)) {
                        throw errorAt(assemblyFile, firstPassParser.lineNumber(), "Label already defined: " + label);
                    }
                    symbolTable.put(label, romAddress);
                } else {
                    romAddress++;
                }
                firstPassParser.advance();
            }
        }

        // Second pass to actually write the binary code from the assembly program:
        try (Parser parser = new Parser(assemblyFile);
             BufferedWriter writer = new BufferedWriter(new FileWriter(hackFile))) {

            while (parser.hasMoreLines()) {
                Parser.InstructionType type = parser.instructionType();
                String code = null;

                try {
                    if (type == Parser.InstructionType.A_INSTRUCTION) {
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
                    else if (type == Parser.InstructionType.C_INSTRUCTION) {
                        code = "111" + Code.comp(parser.comp()) +
                                       Code.dest(parser.dest()) +
                                       Code.jump(parser.jump());
                    }
                } catch (IllegalArgumentException e) {
                    // add the file name and line number to errors from convertToBinary, parseInt and Code:
                    throw errorAt(assemblyFile, parser.lineNumber(), e.getMessage());
                }

                if (code != null) {
                    writer.write(code);
                    writer.newLine();
                }
                parser.advance();
            }
        }
    }
}
