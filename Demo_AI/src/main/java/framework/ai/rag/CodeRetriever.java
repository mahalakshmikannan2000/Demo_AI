package framework.ai.rag;                                    // FIX 1: RAG layer package

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static io.restassured.RestAssured.given;
// FIX 4: RagClient / RetrievedChunk imports removed — same package now

public class CodeRetriever {                                 // FIX 2: correct class name

    private static final Logger logger = LoggerFactory.getLogger(CodeRetriever.class);   // FIX 3
    private static final int N_RESULTS = 3;
    private static final int MAX_CLASS_CHARS = 8000;

    private record ChunkPiece(int chunk, String document) {}

    public static List<RetrievedChunk> retrieve(String query) {
        List<RetrievedChunk> results = new ArrayList<>();    // FIX 5: generic type
        try {
            String collectionID = RagClient.getorCreateCollection();
            if (collectionID == null) {
                logger.warn("[RAG] No collection — is ChromaDB running?");
                return results;                              // FIX 6: empty list, never null
            }
            List<Double> queryVector = RagClient.embed("query", query);
            if (queryVector == null) {
                logger.warn("[RAG] Could not embed query — skipping retrieval");
                return results;                              // FIX 6
            }

            JsonArray vectorArray = new JsonArray();
            for (Double value : queryVector) {
                vectorArray.add(value);
            }
            JsonArray queryEmbedding = new JsonArray();
            queryEmbedding.add(vectorArray);

            JsonArray includes = new JsonArray();            // FIX 7: duplicate line removed
            includes.add("documents");
            includes.add("metadatas");
            includes.add("distances");

            JsonArray allowed = new JsonArray();
            allowed.add("page");
            allowed.add("test");
            allowed.add("framework");
            JsonObject inFilter = new JsonObject();
            inFilter.add("$in", allowed);
            JsonObject where = new JsonObject();
            where.add("category", inFilter);

            JsonObject body = new JsonObject();
            body.add("query_embeddings", queryEmbedding);
            body.addProperty("n_results", N_RESULTS);        // FIX 8: use the constant
            body.add("include", includes);
            body.add("where", where);

            String endpoint = RagClient.COLLECTIONS_URL + "/" + collectionID + "/query";
            logger.debug("[RAG] Query URL: {}", endpoint);  // FIX 9: debug, not info
            Response response = given().contentType(ContentType.JSON).body(body.toString()).when().post(endpoint);

            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                logger.error("[RAG] Query failed. Status: {} Body: {}", response.getStatusCode(), response.asString());
                return results;                              // FIX 6
            }

            JsonObject json = JsonParser.parseString(response.asString()).getAsJsonObject();
            // /query → list of lists → step into [0]
            JsonArray ids = json.getAsJsonArray("ids").get(0).getAsJsonArray();
            JsonArray distances = json.getAsJsonArray("distances").get(0).getAsJsonArray();
            JsonArray documents = json.getAsJsonArray("documents").get(0).getAsJsonArray();
            JsonArray metadatas = json.getAsJsonArray("metadatas").get(0).getAsJsonArray();

            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i).getAsString();
                JsonObject meta = metadatas.get(i).getAsJsonObject();
                String className = meta.has("className")
                        ? meta.get("className").getAsString()
                        : id.split("#")[0];
                RetrievedChunk chunk = new RetrievedChunk(
                        id, documents.get(i).getAsString(), className, distances.get(i).getAsDouble());
                results.add(chunk);
                logger.info("[RAG] #{} {} (class {}, distance {})", i + 1, id, className, chunk.distance());
            }
        } catch (Exception e) {
            logger.error("[RAG] Retrieval failed — continuing without code context", e);   // FIX 10: leftover comment removed
        }
        return results;
    }

    public static String retrieveRelevantCode(String query) {
        List<RetrievedChunk> results = retrieve(query);
        return results.isEmpty() ? "" : results.get(0).document();
    }

    public static String getFullClass(String className) {
        try {
            String collectionId = RagClient.getorCreateCollection();
            if (collectionId == null) {
                return "";
            }

            JsonObject where = new JsonObject();
            where.addProperty("className", className);

            JsonArray include = new JsonArray();
            include.add("documents");
            include.add("metadatas");

            JsonObject body = new JsonObject();
            body.add("where", where);
            body.add("include", include);

            String endpoint = RagClient.COLLECTIONS_URL + "/" + collectionId + "/get";
            Response response = given().contentType(ContentType.JSON).body(body.toString()).when().post(endpoint);

            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                logger.error("[RAG] Get failed. Status: {} Body: {}", response.getStatusCode(), response.asString());
                return "";
            }

            JsonObject json = JsonParser.parseString(response.asString()).getAsJsonObject();
            // /get → FLAT lists → NO .get(0)
            JsonArray documents = json.getAsJsonArray("documents");
            JsonArray metadatas = json.getAsJsonArray("metadatas");

            List<ChunkPiece> pieces = new ArrayList<>();
            for (int i = 0; i < documents.size(); i++) {
                JsonObject meta = metadatas.get(i).getAsJsonObject();
                int chunkNo = meta.has("chunk") ? meta.get("chunk").getAsInt() : 1;
                pieces.add(new ChunkPiece(chunkNo, documents.get(i).getAsString()));
            }
            pieces.sort(Comparator.comparingInt(ChunkPiece::chunk));

            StringBuilder full = new StringBuilder();
            for (ChunkPiece piece : pieces) {
                full.append(piece.document()).append("\n");
            }

            String result = full.toString();
            if (result.length() > MAX_CLASS_CHARS) {
                logger.info("[RAG] {} is {} chars — capped at {}", className, result.length(), MAX_CLASS_CHARS);
                result = result.substring(0, MAX_CLASS_CHARS);
            }
            logger.info("[RAG] Reassembled {} from {} chunk(s), {} chars", className, pieces.size(), result.length());
            return result;

        } catch (Exception e) {                              // FIX 10: blank lines removed
            logger.error("[RAG] getFullClass failed for {}", className, e);
            return "";
        }
    }

    // ---------------- Test ----------------
    public static void main(String[] args) {
        List<RetrievedChunk> results = retrieve(
                "Value is Not entered on Field expected [true] but found [false] TC001_DEMO_Successful_Login");
        logger.info("Results: {}", results.size());

        String webActions = getFullClass("framework.utils.WebActions");
        logger.info("WebActions length: {}\nFirst 200:\n{}",
                webActions.length(), webActions.substring(0, Math.min(200, webActions.length())));

        String loginPage = getFullClass("app.web.DemoLoginPage");
        logger.info("DemoLoginPage length: {}\nFirst 200:\n{}",
                loginPage.length(), loginPage.substring(0, Math.min(200, loginPage.length())));
    }
}