import java.io.File;
import java.io.IOException;

public class JackAnalyzer {
    private static String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        return (dotIndex != -1) ? fileName.substring(0, dotIndex) : fileName;
    }

    private static File[] getAllJackFiles(String source) {
        File sourceFile = new File(source);

        if (sourceFile.isFile() && source.endsWith(".jack")) {
            return new File[] {sourceFile};
        } else if (sourceFile.isDirectory()) {
            File[] jackFiles = sourceFile.listFiles((dir, name) -> name.endsWith(".jack"));
            if (jackFiles == null || jackFiles.length == 0) {
                System.out.println("No .jack files found in the folder!");
                return new File[0];
            }
            return jackFiles;
        } else {
            System.out.println("Invalid file or folder!");
            return new File[0];
        }
    }

    private static String getTargetFilePath(String source) {
        File sourceFile = new File(source);
        if (sourceFile.isDirectory()) {
            String dirPath = sourceFile.getAbsolutePath();
            String baseName = sourceFile.getName();
            return new File(dirPath, baseName + ".xml").getAbsolutePath();
        } else {
            return getBaseName(source) + ".xml";
        }
    }

    public static void main(String[] args) {
        String source = args[0];
        File[] jackFiles = getAllJackFiles(source);

        for (File jackFile : jackFiles) {
            String path = jackFile.getAbsolutePath();
            String target = getTargetFilePath(path);

            try {
                JackTokenizer tokenizer = new JackTokenizer(path);
                CompilationEngine compiler = new CompilationEngine(tokenizer, target);
                compiler.compileClass();
                System.out.println("Processed: " + jackFile.getName());
            } catch (Exception e) {
                System.err.println("Error processing file " + jackFile.getName() +
                                   ": " + e.getMessage());
            }
        }
    }
}