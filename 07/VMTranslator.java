import java.io.IOException;

public class VMTranslator {

    public static void main(String[] args) throws IOException {
        String source = args[0];

        int dotIndex = source.indexOf(".");
        String filename = source.substring(0, dotIndex);
        String target = filename + ".asm";

        Parser parser = new Parser(source);
        CodeWriter codeWriter = new CodeWriter(target);

        while (parser.hasMoreLines()) {
            String commandType = parser.commandType();
            if (commandType.equals("C_ARITHMETIC")) {
                codeWriter.writeArithmetic(parser.arg1());
            }
            else if (commandType.equals("C_PUSH") || commandType.equals("C_POP")) {
                codeWriter.writePushPop(parser.command(), parser.arg1(), parser.arg2());
            }

            parser.advance();
        }

        parser.close();
        codeWriter.writeFinalInfiniteLoop();
        codeWriter.close();
    }
}