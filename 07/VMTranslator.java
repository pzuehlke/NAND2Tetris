import java.io.IOException;

public class VMTranslator {

    public static void main(String[] args) throws IOException {
        String source = args[0];
        String target = source.substring(0, source.lastIndexOf(".")) + ".asm";

        Parser parser = new Parser(source);
        CodeWriter codeWriter = new CodeWriter(target);

        while (parser.hasMoreLines()) {
            String commandType = parser.commandType();
            if (commandType.equals("C_ARITHMETIC")) {
                codeWriter.writeArithmetic(parser.arg1());
            } else if (commandType.equals("C_PUSH") || commandType.equals("C_POP")) {
                codeWriter.writePushPop(parser.command(), parser.arg1(), parser.arg2());
            } else {
                throw new IllegalArgumentException("Unsupported command: " + parser.command());
            }

            parser.advance();
        }

        parser.close();
        codeWriter.writeFinalInfiniteLoop();
        codeWriter.close();
    }
}
