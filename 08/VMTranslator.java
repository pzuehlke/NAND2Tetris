import java.io.IOException;
import java.io.File;

public class VMTranslator {

    private static String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex != -1) {
            return fileName.substring(0, dotIndex);
        } else {
            return fileName;
        }  
    }

    private static File[] getAllVMFiles(String source) {
        File[] vmFiles;
        File sourceFile = new File(source);

        if (sourceFile.isFile() && source.endsWith(".vm")) {
            // It's a single .vm file
            vmFiles = new File[] {sourceFile};
        } else if (sourceFile.isDirectory()) {
            // It's a folder; get all .vm files
            vmFiles = sourceFile.listFiles((dir, name) -> name.endsWith(".vm"));
            if (vmFiles == null || vmFiles.length == 0) {
                System.out.println("No .vm files found in the folder!");
            }
        } else {
            System.out.println("Invalid file or folder!");
            vmFiles = new File[0];
        }

        return vmFiles;
    }

    private static String getTargetFilePath(String source) {
        File sourceFile = new File(source);
        String target;

        if (sourceFile.isDirectory()) {
            // If source is a directory, place the target .asm file inside this directory:
            String dirPath = sourceFile.getAbsolutePath();
            String baseName = source;  // Name of the directory
            target = new File(dirPath, baseName + ".asm").getAbsolutePath();
        } else {
            // If source is a file, place the .asm file in the same directory as the source:
            target = getBaseName(source) + ".asm";
        }

        return target;
    }

    public static void main(String[] args) throws IOException {
        String source = args[0];
        File[] vmFiles = getAllVMFiles(source);

        String target = getTargetFilePath(source);
        CodeWriter codeWriter = new CodeWriter(target);

        // Bootstrapping code
        if (vmFiles.length > 1 || new File(source).isDirectory()) {
            codeWriter.writeInit();
        }

        for (File file : vmFiles) {
            Parser parser = new Parser(file.getAbsolutePath());
            String fileName = getBaseName(file.getName());
            codeWriter.setFileName(fileName);

            while (parser.hasMoreLines()) {
                String commandType = parser.commandType();
                if (commandType.equals("C_ARITHMETIC")) {
                    codeWriter.writeArithmetic(parser.arg1());
                } else if (commandType.equals("C_PUSH") || commandType.equals("C_POP")) {
                    codeWriter.writePushPop(parser.command(), parser.arg1(), parser.arg2());
                } else if (commandType.equals("C_LABEL")) {
                    codeWriter.writeLabel(parser.arg1());
                } else if (commandType.equals("C_GOTO")) {
                    codeWriter.writeGoTo(parser.arg1());
                } else if (commandType.equals("C_IF")) {
                    codeWriter.writeIf(parser.arg1());
                } else if (commandType.equals("C_FUNCTION")) {
                    codeWriter.writeFunction(parser.arg1(), parser.arg2());
                } else if (commandType.equals("C_CALL")) {
                    codeWriter.writeCall(parser.arg1(), parser.arg2());
                } else if (commandType.equals("C_RETURN")) {
                    codeWriter.writeReturn();
                }

                parser.advance();
            }
            parser.close();
        }

        codeWriter.writeFinalInfiniteLoop();
        codeWriter.close();
    }
}
