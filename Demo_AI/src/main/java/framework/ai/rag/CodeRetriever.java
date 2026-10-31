package framework.ai.rag;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.restassured.RestAssured.given;

public class CodeRetriever {


    private static final Logger logger = LoggerFactory.getLogger(CodeRetriever.class);

    public static String retrieveRelevantCode(String query) {
        try {
            String collectionID = RagClient.getorCreateCollection();
            if (collectionID == null) {
                logger.warn("[RAG] No collection — is ChromaDB running?");
                return "";
            }
            List<Double> queryVector = RagClient.embed("query", query);
            if (queryVector == null) {
                logger.warn("[RAG] Could not embed query — skipping retrieval");
                return "";
            }

            JsonArray vectorArray = new JsonArray();
            for (Double result : queryVector) {
                vectorArray.add(result);
            }

            JsonArray queryEmbedding = new JsonArray();
            queryEmbedding.add(vectorArray);

            JsonArray includes = new JsonArray();
            includes.add("documents");
            includes.add("metadatas");
            includes.add("distances");


            JsonArray allowed = new JsonArray();          // 1. innermost: the list
            allowed.add("page");
            allowed.add("test");
            allowed.add("framework");

            JsonObject inFilter = new JsonObject();       // 2. {"$in": [...]}
            inFilter.add("$in", allowed);

            JsonObject where = new JsonObject();          // 3. {"category": {...}}
            where.add("category", inFilter);

            JsonObject body = new JsonObject();
            body.add("query_embeddings", queryEmbedding);
            body.addProperty("n_results", 3);
            body.add("include", includes);
            body.add("where",where);


            String endpoint = RagClient.COLLECTIONS_URL + "/" + collectionID + "/query";
            logger.info("Endpoint of Query {}", endpoint);
            Response response = given().contentType(ContentType.JSON).body(body.toString()).when().post(endpoint);

            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                logger.error("Query failed. Status Code: {}", response.getStatusCode());
                logger.error("Response Body: {}", response.asString());
                return "";
            }

            JsonObject jsonObject = JsonParser.parseString(response.asString()).getAsJsonObject();

            // FIX 1: step into the INNER list with .get(0) — outer list = 1 per query
            JsonArray idJsonArray = jsonObject.get("ids").getAsJsonArray().get(0).getAsJsonArray();
            JsonArray distanceJsonArray = jsonObject.get("distances").getAsJsonArray().get(0).getAsJsonArray();
            JsonArray documentsJsonArray = jsonObject.get("documents").getAsJsonArray().get(0).getAsJsonArray();

            if (idJsonArray.isEmpty()) {                                             // guard: empty collection
                logger.warn("[RAG] Query returned no results");
                return "";
            }

            for (int i = 0; i < idJsonArray.size(); i++) {
                logger.info("[RAG] #{} {} (distance {})",
                        i + 1,
                        idJsonArray.get(i).getAsString(),
                        distanceJsonArray.get(i).getAsDouble());
            }

            return documentsJsonArray.get(0).getAsString();                          // closest result's code

        } catch (Exception e) {
            logger.error("[RAG] Retrieval failed — continuing without code context", e);
            return "";
        }
    }

    public static void main(String[] args) {
        runQuery("Unable to locate element: {method=id, value=user-name_broken} TC001_DEMO_Successful_Login");
        runQuery("Failed to decrypt secret key AES GCM dataDecrypt");
    }

    private static void runQuery(String query) {
        logger.info("===== QUERY: {} =====", query);
        String code = retrieveRelevantCode(query);
        logger.info("Returned code length: {}", code.length());
        logger.info("First 200 chars:\n{}", code.substring(0, Math.min(200, code.length())));
    }
}
