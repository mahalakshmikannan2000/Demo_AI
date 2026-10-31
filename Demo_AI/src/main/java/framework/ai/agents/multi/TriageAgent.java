package framework.ai.agents.multi;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static framework.ai.prompts.FewShotExamples.EXAMPLE_INPUT_2;
import static framework.ai.prompts.SystemPrompts.TRIAGE_PROMPT;

public class TriageAgent {

    public static void run(FailureContext ctx){
        List<Map<String, String>> listOfMap = new ArrayList<>();

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", TRIAGE_PROMPT);

        JsonObject userInput = new JsonObject();
        userInput.addProperty("testName",ctx.getTestName());
        userInput.addProperty("errorMessage",ctx.getErrorMessage());
        String userContent  = userInput.toString();

        Map<String, String> usercontext = new HashMap<>();
        systemMessage.put("role", "user");
        systemMessage.put("content", userContent);

        listOfMap.add(systemMessage);
        listOfMap.add(usercontext);
    }
}
