package framework.ai.agents;

import com.google.gson.Gson;
import framework.ai.rag.CodeRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static framework.ai.prompts.FewShotExamples.*;
import static framework.ai.prompts.SystemPrompts.FAILURE_ANALYSIS_PROMPT;

public class FailureAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(FailureAnalysisService.class);

// Once Real AI Configured then this should be enabled
private static final int MAX_CODE_CHARS = 6000;

    public static String analyseAndBuildMessages() {
        try {
            Map<String, String> promptFormatInput = new HashMap<>();
            List<Map<String, String>> listOfMap = new ArrayList<>();          // FIX 4: unused queryInput removed

            Map<String, String> latestFailure = AllureResultReader.getLatestFailure();
            promptFormatInput.put("Test", latestFailure.get("testName"));
            promptFormatInput.put("Error", latestFailure.get("errorMessage"));
            promptFormatInput.put("File", latestFailure.get("fileToFix"));
            promptFormatInput.put("Framework", "Selenium Java TestNG");

            Gson gson = new Gson();
            String jsonString = gson.toJson(promptFormatInput);

            String query = latestFailure.get("errorMessage") + " " + latestFailure.get("testName");

            String relevantCode = CodeRetriever.retrieveRelevantCode(query);

            if (relevantCode.length() > MAX_CODE_CHARS) {
                relevantCode = relevantCode.substring(0, MAX_CODE_CHARS);
            }

            String userContent;
            if (!relevantCode.isEmpty()) {
                userContent = jsonString
                        + "\n\nRelevant source code (retrieved from codebase):\n"
                        + relevantCode;
                logger.info("[RAG] Retrieved {} chars of code context", relevantCode.length());   // FIX 3
            } else {
                userContent = jsonString;
                logger.info("[RAG] No code context — analysing without RAG");                    // FIX 3
            }

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", FAILURE_ANALYSIS_PROMPT);

            Map<String, String> userInputExp = new HashMap<>();
            userInputExp.put("role", "user");
            userInputExp.put("content", EXAMPLE_INPUT_2);

            Map<String, String> userOutputExp = new HashMap<>();
            userOutputExp.put("role", "assistant");
            userOutputExp.put("content", EXAMPLE_OUTPUT_2);

            Map<String, String> actualMessage = new HashMap<>();
            actualMessage.put("role", "user");
            actualMessage.put("content", userContent);

            listOfMap.add(systemMessage);
            listOfMap.add(userInputExp);
            listOfMap.add(userOutputExp);
            listOfMap.add(actualMessage);

            return OllamaClient.chat(listOfMap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//public static String analyseAndBuildMessages() {
//        try {
//            Map<String, String> promptFormatInput = new HashMap<>();
//            List<Map<String, String>> listOfMap = new ArrayList<>();
//
//            Map<String, String> latestFailure = AllureResultReader.getLatestFailure();
//            promptFormatInput.put("Test", latestFailure.get("testName"));
//            promptFormatInput.put("Error", latestFailure.get("errorMessage"));
//            promptFormatInput.put("File", latestFailure.get("fileToFix"));
//            promptFormatInput.put("Framework", "Selenium Java TestNG");
//            Gson gson = new Gson();
//            String jsonString = gson.toJson(promptFormatInput);
//
//            Map<String, String> systemMessage = new HashMap<>();
//            systemMessage.put("role","system");
//            systemMessage.put("content",FAILURE_ANALYSIS_PROMPT);
//            Map<String, String> userInputExp = new HashMap<>();
//            userInputExp.put("role","user");
//            userInputExp.put("content",EXAMPLE_INPUT_1);
//            Map<String, String> userOutputExp = new HashMap<>();
//            userOutputExp.put("role","assistant");
//            userOutputExp.put("" +
//                    "",EXAMPLE_OUTPUT_1);
//            Map<String, String> actualMessage = new HashMap<>();
//            actualMessage.put("role","user");
//            actualMessage.put("content",jsonString);
//            listOfMap.add(systemMessage);
//            listOfMap.add(userInputExp);
//            listOfMap.add(userOutputExp);
//            listOfMap.add(actualMessage);
//            logger.info("[AI-MOCK] Messages assembled. Returning mock response");
//            return EXAMPLE_OUTPUT_1;
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//    }

}
