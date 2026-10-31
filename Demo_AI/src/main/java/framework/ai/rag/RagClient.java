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

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;

public class RagClient {

    private static final Logger logger = LoggerFactory.getLogger(RagClient.class);
    private static final String COLLECTION_NAME = "code_index";
    private static final int LARGE_FILE_CHARS = 8000;
    public static String CHROMA_BASE_URL;
    private static String EMBED_MODEL;
    private static String OLLAMA_BASE_URL;
    private static final RestAssuredConfig OLLAMA_CONFIG = RestAssuredConfig.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", 10_000)   // 10 s: fail fast if Ollama is down
                    .setParam("http.socket.timeout", 180_000));    // 3 min: allow model cold start


    static {
        Dotenv dotenv = Dotenv.load();
        OLLAMA_BASE_URL = dotenv.get("OLLAMA_BASE_URL");
        CHROMA_BASE_URL = dotenv.get("CHROMA_BASE_URL");
        EMBED_MODEL = dotenv.get("OLLAMA_EMBED_MODEL");
    }

    public static final String COLLECTIONS_URL = CHROMA_BASE_URL + "/api/v2/tenants/default_tenant/databases/default_database/collections";

    private RagClient() {
    }

    public static String getorCreateCollection() {
        String requestBody = """
                {
                "name":"code_index",
                "get_or_create":true
                }
                """;
        Response response = given().contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post(COLLECTIONS_URL);
        int statusCode = response.getStatusCode();
        if (statusCode < 200 || statusCode >= 300) {
            logger.error("Collection creation failed. Status Code: {}", statusCode);
            logger.error("Response Body: {}", response.asString());
            return null;
        }
        JsonObject jsonObject = JsonParser.parseString(response.asString()).getAsJsonObject();

        String id = jsonObject.get("id").getAsString();

        return id;
    }

    public static List<Double> embed(String id, String text) {

        if (text.length() >= LARGE_FILE_CHARS) {
            logger.warn("[RAG] {} is large ({} chars) — may be truncated", id, text.length());
        }

        JsonObject body = new JsonObject();
        body.addProperty("model", EMBED_MODEL);
        body.addProperty("input", text);
        body.addProperty("truncate", false);
        Response response = given().config(OLLAMA_CONFIG).contentType(ContentType.JSON).body(body.toString()).when().post(OLLAMA_BASE_URL + "/api/embed");

        if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
            logger.error("Embedding failed for {}. Status Code: {}", id, response.getStatusCode());
            logger.error("Response Body: {}", response.asString());
            return null;
        }

        JsonArray embeddings = JsonParser.parseString(response.asString()).getAsJsonObject().getAsJsonArray("embeddings");
        JsonArray vectorJson = embeddings.get(0).getAsJsonArray();
        List<Double> vector = new ArrayList<>(vectorJson.size());
        for (int i = 0; i < vectorJson.size(); i++) {
            vector.add(vectorJson.get(i).getAsDouble());
        }
        return vector;

    }



}