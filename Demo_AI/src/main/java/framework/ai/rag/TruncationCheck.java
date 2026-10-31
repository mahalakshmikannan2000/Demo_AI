package framework.ai.rag;

import com.google.gson.JsonObject;
import io.github.cdimascio.dotenv.Dotenv;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;

import static io.restassured.RestAssured.given;

public class TruncationCheck {

    private static final Logger logger = LoggerFactory.getLogger(TruncationCheck.class);
    private static final Dotenv dotenv = Dotenv.load();
    private static final String EMBED_URL = dotenv.get("OLLAMA_BASE_URL") + "/api/embed";
    private static final String MODEL = dotenv.get("OLLAMA_EMBED_MODEL");

    public static void main(String[] args) throws Exception {
        // Small file (should always pass) and large file (suspected truncation)
        check("DemoLoginPage", "src/main/java/app/web/DemoLoginPage.java", null);
        check("WebActions",    "src/main/java/framework/utils/WebActions.java", null);

        // Same large file, but asking Ollama for a bigger context window (Step B option 1)
        check("WebActions + num_ctx 8192", "src/main/java/framework/utils/WebActions.java", 8192);
    }

    private static void check(String label, String filePath, Integer numCtx) throws Exception {
        String code = Files.readString(Paths.get(filePath));

        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.addProperty("input", code);
        body.addProperty("truncate", false);          // NEW: error instead of silent cut

        if (numCtx != null) {                         // NEW: optional bigger context
            JsonObject options = new JsonObject();
            options.addProperty("num_ctx", numCtx);
            body.add("options", options);
        }

        Response response = given()
                .contentType(ContentType.JSON)
                .body(body.toString())
                .when()
                .post(EMBED_URL);

        String result = response.asString();
        logger.info("[{}] chars={} → status {} | {}",
                label,
                code.length(),
                response.getStatusCode(),
                response.getStatusCode() == 200
                        ? "OK (fits — no truncation)"
                        : result.substring(0, Math.min(200, result.length())));
    }
}