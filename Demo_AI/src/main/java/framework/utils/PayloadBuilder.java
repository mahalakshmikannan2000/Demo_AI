package framework.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Loads JSON payload/config files from disk (e.g. the base-URL map used by APIClient.buildEndPoint).
 */
public class PayloadBuilder {

    private PayloadBuilder() {
        throw new UnsupportedOperationException("Payload Builder class — do not instantiate.");
    }

    /*
     * Method Description : Loads and parses a JSON file into a JsonObject
     * Input Parameter(s) if any : String path - Absolute or relative path to the JSON file
     * Output Parameter(s) if any : JsonObject - Parsed JSON object
     * Throws : IOException if file reading or parsing fails
     */
    public static JsonObject loadJsonFile(String path) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(path)));
        return JsonParser.parseString(content).getAsJsonObject();
    }
}
