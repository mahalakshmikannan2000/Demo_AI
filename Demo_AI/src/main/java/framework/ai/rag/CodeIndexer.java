package framework.ai.rag;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

import static framework.ai.rag.RagClient.COLLECTIONS_URL;
import static io.restassured.RestAssured.given;

// Run with: chroma run --host localhost --port 8000  (and Ollama running)
public class CodeIndexer {

    private static final Logger logger = LoggerFactory.getLogger(CodeIndexer.class);

    private static final List<String> EXCLUDED_PREFIXES =
            List.of("framework.ai.prompts", "framework.ai.rag", "framework.ai.agents");

    // NEW: files bigger than this are split into chunks (safe margin under 2048 tokens)
    private static final int MAX_CHUNK_CHARS = 5000;

    public static void main(String[] args) {
        String collectionId = RagClient.getorCreateCollection();
        if (collectionId == null) {
            logger.error("Cannot continue without a collection — is ChromaDB running?");
            return;
        }
        logger.info("Collection ID: {}", collectionId);

        List<CodeFile> allFiles = new ArrayList<>();
        allFiles.addAll(scanJavaFiles(Paths.get("src", "main", "java"), "main"));
        allFiles.addAll(scanJavaFiles(Paths.get("src", "test", "java"), "test"));
        logger.info("Files found: {}", allFiles.size());

        int recordsOk = 0;
        List<String> failed = new ArrayList<>();
        List<String> chunkedClassIds = new ArrayList<>();               // NEW: for cleanup

        for (CodeFile file : allFiles) {
            try {
                String code = Files.readString(file.path());

                List<String> pieces;
                if (code.length() <= MAX_CHUNK_CHARS) {
                    pieces = List.of(code);
                } else {
                    pieces = splitIntoChunks(code);
                }
                if (pieces.size() > 1) {
                    chunkedClassIds.add(file.id());
                    logger.info("[RAG] {} ({} chars) split into {} chunks",
                            file.id(), code.length(), pieces.size());
                }
                for (int i = 0; i < pieces.size(); i++) {
                    // NEW: keep the plain class ID for single records, add #partN for chunks
                    String recordId = pieces.size() == 1 ? file.id() : file.id() + "#part" + (i + 1);
                    String text = pieces.get(i);

                    List<Double> vector = RagClient.embed(recordId, text);
                    if (vector == null) {
                        failed.add(recordId);
                        continue;
                    }
                    if (upsert(collectionId, recordId, file, text, vector, i + 1, pieces.size())) {
                        recordsOk++;
                        logger.info("Indexed: {}", recordId);
                    } else {
                        failed.add(recordId);
                    }
                }
            } catch (Exception e) {
                logger.error("Error indexing {}: {}", file.id(), e.getMessage());
                failed.add(file.id());
            }
        }

        // NEW: migration cleanup — remove old whole-file records of classes that are now chunked
        deleteRecords(collectionId, chunkedClassIds);

        logger.info("Indexed {} records ({} failed)", recordsOk, failed.size());
        if (!failed.isEmpty()) {
            logger.warn("Failed: {}", failed);
        }
        logger.info("ChromaDB record count: {}", getCount(collectionId));
    }

    // ---------------- Scanning (Part 1, with the 3 fixes) ----------------
    public static List<CodeFile> scanJavaFiles(Path root, String source) {
        List<CodeFile> codeFiles = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            logger.warn("Folder not found, skipping: {}", root);
            return codeFiles;
        }
        int skipped = 0;                                                  // FIX: declared BEFORE the loop
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> javaFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".java"))
                    .toList();
            for (Path filePath : javaFiles) {
                String relative = root.relativize(filePath).toString();
                String withDots = relative.replace(File.separatorChar, '.');
                String id = withDots.substring(0, withDots.length() - ".java".length());

                if (isExcluded(id)) {
                    skipped++;
                    continue;
                }
                codeFiles.add(new CodeFile(id, filePath, source));
            }
        } catch (IOException e) {
            logger.error("Failed to scan folder: {}", root, e);
        }
        logger.info("Skipped {} AI tooling files in {}", skipped, root);    // FIX: after loop, {} placeholders
        return codeFiles;
    }

    private static boolean isExcluded(String id) {
        for (String prefix : EXCLUDED_PREFIXES) {
            if (id.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String categoryOf(String id) {
        if (id.startsWith("app.")) {
            return "page";
        } else if (id.startsWith("tests.")) {
            return "test";                                                // FIX: was "page"
        } else {
            return "framework";
        }
    }

    // ---------------- NEW: Chunking ----------------
    static List<String> splitIntoChunks(String code) {
        StringBuilder header = new StringBuilder();     // package + class line + fields
        List<String> members = new ArrayList<>();       // each method (or static block) as text
        StringBuilder pending = new StringBuilder();    // annotations/comments waiting for their member
        StringBuilder current = new StringBuilder();    // the method being read right now
        boolean inMember = false;
        int depth = 0;

        for (String line : code.split("\\R")) {          // \R = any line break (\n or \r\n on Windows)
            int depthBefore = depth;
            for (char c : line.toCharArray()) {
                if (c == '{') depth++;
                else if (c == '}') depth--;
            }
            String trimmed = line.trim();

            if (inMember) {                              // inside a method: keep collecting
                current.append(line).append("\n");
                if (depth == 1) {                        // back to class level → method finished
                    members.add(current.toString());
                    current = new StringBuilder();
                    inMember = false;
                }
            } else if (depthBefore == 0) {               // outside the class: package, imports, class line
                if (!trimmed.startsWith("import ") && depth <= 1) {
                    header.append(line).append("\n");
                }
            } else if (depthBefore == 1 && depth > 1) {  // a method/block starts on this line
                current.append(pending).append(line).append("\n");
                pending = new StringBuilder();
                inMember = true;
            } else if (depthBefore == 1 && depth == 1 && trimmed.contains("{")) {
                members.add(pending + line + "\n");      // one-line method, e.g. getX() { return x; }
                pending = new StringBuilder();
            } else if (depthBefore == 1 && depth == 1 && trimmed.endsWith(";")) {
                header.append(pending).append(line).append("\n");   // a field (with its @FindBy etc.)
                pending = new StringBuilder();
            } else if (depthBefore == 1 && depth == 1) {
                pending.append(line).append("\n");      // annotation / comment / blank line
            }
            // depthBefore == 1 && depth == 0 → the class's closing brace: skipped, added back per chunk
        }

        if (members.isEmpty()) {                         // nothing to split (interface, enum…)
            return List.of(code);
        }

        // Group whole methods into chunks: header + methods + closing brace
        List<String> chunks = new ArrayList<>();
        StringBuilder chunk = new StringBuilder();
        for (String member : members) {
            boolean wouldOverflow = header.length() + chunk.length() + member.length() + 2 > MAX_CHUNK_CHARS;
            if (chunk.length() > 0 && wouldOverflow) {
                chunks.add(header + chunk.toString() + "}\n");
                chunk = new StringBuilder();
            }
            chunk.append(member).append("\n");
        }
        if (chunk.length() > 0) {
            chunks.add(header + chunk.toString() + "}\n");
        }
        return chunks;
    }

    // ---------------- Upsert (NEW: recordId + chunk metadata) ----------------
    private static boolean upsert(String collectionId, String recordId, CodeFile file,
                                  String text, List<Double> vector, int chunkNo, int totalChunks) {
        String endPoint = COLLECTIONS_URL + "/" + collectionId + "/upsert";

        JsonArray ids = new JsonArray();
        ids.add(recordId);                                                // NEW: may be Class#partN

        JsonArray documents = new JsonArray();
        documents.add(text);

        JsonArray oneVector = new JsonArray();
        for (Double value : vector) {
            oneVector.add(value);
        }
        JsonArray embeddings = new JsonArray();
        embeddings.add(oneVector);

        JsonObject meta = new JsonObject();
        meta.addProperty("filename", file.path().getFileName().toString());
        meta.addProperty("source", file.source());
        meta.addProperty("path", file.path().toString());
        meta.addProperty("category", categoryOf(file.id()));
        meta.addProperty("className", file.id());                       // NEW: parent class
        meta.addProperty("chunk", chunkNo);                              // NEW
        meta.addProperty("totalChunks", totalChunks);                    // NEW
        JsonArray metadatas = new JsonArray();
        metadatas.add(meta);

        JsonObject body = new JsonObject();
        body.add("ids", ids);
        body.add("documents", documents);
        body.add("embeddings", embeddings);
        body.add("metadatas", metadatas);

        Response response = given().contentType(ContentType.JSON).body(body.toString()).when().post(endPoint);
        if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
            logger.error("Upsert failed for {}. Status Code: {}", recordId, response.getStatusCode());
            logger.error("Response Body: {}", response.asString());
            return false;
        }
        return true;
    }

    // ---------------- NEW: delete records by ID (migration cleanup) ----------------
    private static void deleteRecords(String collectionId, List<String> idsToDelete) {
        if (idsToDelete.isEmpty()) {
            return;
        }
        JsonArray ids = new JsonArray();
        idsToDelete.forEach(ids::add);
        JsonObject body = new JsonObject();
        body.add("ids", ids);

        Response response = given().contentType(ContentType.JSON).body(body.toString())
                .when().post(COLLECTIONS_URL + "/" + collectionId + "/delete");
        if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
            logger.error("Delete failed. Status Code: {} Body: {}", response.getStatusCode(), response.asString());
        } else {
            logger.info("[RAG] Removed old whole-file records: {}", idsToDelete);
        }
    }

    private static int getCount(String collectionId) {
        Response response = given().when().get(COLLECTIONS_URL + "/" + collectionId + "/count");
        if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
            logger.error("Count failed. Status Code: {}", response.getStatusCode());
            return -1;
        }
        return Integer.parseInt(response.asString().trim());
    }
}