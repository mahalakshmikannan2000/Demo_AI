package framework.ai.rag;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cdimascio.dotenv.Dotenv;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
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
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

// chroma run --host localhost --port 8000

public class CodeIndexer {
    private static final Logger logger = LoggerFactory.getLogger(CodeIndexer.class);
    private static String chromaBaseURL;
    private static String nomicEmbedTextModel;
    private static String ollamaBaseUrl;
    private static final int LARGE_FILE_CHARS = 8000;

    private static final RestAssuredConfig OLLAMA_CONFIG = RestAssuredConfig.config()
                    .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", 10_000)   // 10 s: fail fast if Ollama is down
                    .setParam("http.socket.timeout", 180_000));    // 3 min: allow model cold start


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

public static void main(String[] args) throws IOException {

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
    CodeFile loginPage = allFiles.stream().filter(f -> f.id().equals("app.web.DemoLoginPage")).findFirst().orElseThrow(() -> new IllegalStateException("DemoLoginPage not found in scan"));
    String code = Files.readString(loginPage.path());

    logger.info("Read {} chars from {}", code.length(), loginPage.id());

    long start = System.currentTimeMillis();
    List<Double> vector = embed(loginPage.id(), code);
    long timeTaken = System.currentTimeMillis() - start;


    if (vector == null) {
        logger.error("No vector returned — check Ollama is running");
        return;
    }
    logger.info("Vector size: {}", vector.size());
    logger.info("First 5: {}", vector.subList(0, 5));
    logger.info("Embed time: {} ms", timeTaken);


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

    private static List<Double> embed(String id,String text){

        if(text.length() >=LARGE_FILE_CHARS){
            logger.warn("[RAG] {} is large ({} chars) — may be truncated", id, text.length());
        }

        JsonObject body = new JsonObject();
        body.addProperty("model",nomicEmbedTextModel);
        body.addProperty("input",text);


        Response response = given().config(OLLAMA_CONFIG).contentType(ContentType.JSON).body(body.toString()).when().post(ollamaBaseUrl+"/api/embed");

        if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
            logger.error("Embedding failed for {}. Status Code: {}", id, response.getStatusCode());
            logger.error("Response Body: {}", response.asString());
            return null;
        }

        JsonArray embeddings = JsonParser.parseString(response.asString()).getAsJsonObject().getAsJsonArray("embeddings");
        System.out.println(embeddings);
        JsonArray vectorJson = embeddings.get(0).getAsJsonArray();
        List<Double> vector = new ArrayList<>(vectorJson.size());
        for (int i = 0; i < vectorJson.size(); i++) {
            vector.add(vectorJson.get(i).getAsDouble());
        }
        return vector;

    }

}
