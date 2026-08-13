package framework.ai.agents;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AllureResultReader {

    public static final Logger logger = LoggerFactory.getLogger(AllureResultReader.class);


    public static Map<String, String> getLatestFailure() {
        Map<String, String> result = new HashMap<>();

        try {

            logger.info("Working directory: {}", System.getProperty("user.dir"));
            logger.info("Allure folder path: {}", new File(System.getProperty("user.dir") + "/allure-results").getAbsolutePath());

            File folder = new File(System.getProperty("user.dir")+"/allure-results");
            File[] files = folder.listFiles((dir, name) -> name.endsWith("-result.json"));
            if (files ==null || files.length == 0) {
                logger.warn("No Result Json present under Allure Result");
                return result;
            } else {
                Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                for (File file : files) {
                    String jsonContent = Files.readString(file.toPath());
                    JsonReader jsonReader = new JsonReader(new StringReader(jsonContent));
                    jsonReader.setLenient(true);
                    JsonObject jsonObject = JsonParser.parseReader(jsonReader).getAsJsonObject();
                    // Now access JSON fields
                    if (jsonObject.get("status").getAsString().equals("failed")) {
                        result.put("fullName", jsonObject.get("fullName").getAsString());
                        result.put("errorMessage",  jsonObject.getAsJsonObject("statusDetails").get("message").getAsString());
                        JsonArray labels = jsonObject.getAsJsonArray("labels");
                        for (JsonElement element : labels) {
                            JsonObject label = element.getAsJsonObject();
                            String name = label.get("name").getAsString();
                            String value = label.get("value").getAsString();
                            if ("subSuite".equals(name)) {
                                String className = value.substring(value.lastIndexOf(".") + 1);
                                result.put("fileToFix", className + ".java");
                            }
                            if ("testMethod".equals(name)) {
                                result.put("testName", value);
                            }
                        }
                    }
                }
            }
            logger.info("Failure details found: {}", result);
        } catch (Exception e) {
            logger.error("Exception While reading Json of Result: {}", e.getMessage(), e);
        }
        return result;
    }
}
