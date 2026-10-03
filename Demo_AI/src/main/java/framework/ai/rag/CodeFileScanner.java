package framework.ai.rag;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class CodeFileScanner {

    private static final Logger logger = LoggerFactory.getLogger(CodeFileScanner.class);
    private static final int LARGE_FILE_CHARS = 8000;

    // FIX 1: returns List<CodeFile> instead of void, so Step 3 can use the data
    // FIX 2: root is a Path, not a String with hardcoded slashes
    public static List<CodeFile> scanJavaFiles(Path root, String source) {
        List<CodeFile> codeFiles = new ArrayList<>();

        // Guard: if a folder doesn't exist, warn and continue instead of crashing
        if (!Files.isDirectory(root)) {
            logger.warn("Folder not found, skipping: {}", root);
            return codeFiles;
        }

        // FIX 3: try-with-resources closes the stream (and its file handles) automatically
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> javaFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".java"))
                    .toList();

            for (Path filePath : javaFiles) {
                String relative = root.relativize(filePath).toString();

                // Works on Windows (\) and Linux/Mac (/)
                String withDots = relative.replace(File.separatorChar, '.');

                String id = withDots.substring(0, withDots.length() - ".java".length());

                codeFiles.add(new CodeFile(id, filePath, source));
            }

        } catch (IOException e) {   // FIX 7: catch the specific exception, and say which folder failed
            logger.error("Failed to scan folder: {}", root, e);
        }

        return codeFiles;
    }

    private static List<Double> embed(String id,String text){

        List<Double> listofVector= new LinkedList<>();
if(text.length() >=LARGE_FILE_CHARS){
    logger.warn("[RAG] {} is large ({} chars) — may be truncated", id, text.length());
}

JsonObject body = new JsonObject();
body.addProperty("model","");

        return listofVector;

    }

    // Debug main — will move into CodeIndexer once Step 2 passes
    public static void main(String[] args) {
        List<CodeFile> allFiles = new ArrayList<>();
        allFiles.addAll(scanJavaFiles(Paths.get("src", "main", "java"), "main"));
        allFiles.addAll(scanJavaFiles(Paths.get("src", "test", "java"), "test"));
        for (CodeFile file : allFiles) {
            logger.info("{} | {} | {}", file.id(), file.path(), file.source());
        }
        logger.info("Total .java files found: {}", allFiles.size());
    }


}