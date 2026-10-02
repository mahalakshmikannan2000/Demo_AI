package framework.ai.rag;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cdimascio.dotenv.Dotenv;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

// chroma run --host localhost --port 8000

public class CodeIndexer {
    private static final Logger logger = LoggerFactory.getLogger(CodeIndexer.class);
    private static String chromaBaseURL;
    private static String nomicEmbedTextModel;
    private static String ollamaBaseUrl;


    static {
        Dotenv dotenv = Dotenv.load();
        ollamaBaseUrl = dotenv.get("OLLAMA_BASE_URL");
        chromaBaseURL = dotenv.get("CHROMA_BASE_URL");
        nomicEmbedTextModel = dotenv.get("OLLAMA_EMBED_MODEL");
    }

    private static String getorCreateCollection() {
        String endpoint = chromaBaseURL + "/api/v2/tenants/default_tenant/databases/default_database/collections";
        String requestBody = """
                {
                "name":"code_index",
                "get_or_create":true
                }
                """;
        Response response = given().contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post(endpoint);
        int statusCode = response.getStatusCode();
        if (statusCode < 200 || statusCode >= 300) {
            logger.error("Collection creation failed. Status Code: {}", statusCode);
            logger.error("Response Body: {}", response.asString());
            return null;
        }
       JsonObject jsonObject =  JsonParser.parseString(response.asString()).getAsJsonObject();

        String id = jsonObject.get("id").getAsString();

        return id;
    }

public static void main(String[] args){

    String id =  getorCreateCollection();
    logger.info("Collection ID: " + id);

    List<CodeFile> allFiles = new ArrayList<>();
    allFiles.addAll(scanJavaFiles(Paths.get("src", "main", "java"), "main"));
    allFiles.addAll(scanJavaFiles(Paths.get("src", "test", "java"), "test"));

    // FIX 8: readable output with separators
    for (CodeFile file : allFiles) {
        logger.info("{} | {} | {}", file.id(), file.path(), file.source());
    }
    logger.info("Total .java files found: {}", allFiles.size());

    }

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
                    .filter(file -> file.toString().endsWith(".java"))   // FIX 4: endsWith, not contains
                    .toList();

            for (Path filePath : javaFiles) {
                // FIX 5: relativize removes the root part for us -> "app\web\DemoLoginPage.java"
                String relative = root.relativize(filePath).toString();

                // Works on Windows (\) and Linux/Mac (/)
                String withDots = relative.replace(File.separatorChar, '.');

                // FIX 6: remove only the LAST 5 characters (".java"), not every ".java" in the text
                String id = withDots.substring(0, withDots.length() - ".java".length());

                codeFiles.add(new CodeFile(id, filePath, source));
            }

        } catch (IOException e) {   // FIX 7: catch the specific exception, and say which folder failed
            logger.error("Failed to scan folder: {}", root, e);
        }

        return codeFiles;
    }

}
