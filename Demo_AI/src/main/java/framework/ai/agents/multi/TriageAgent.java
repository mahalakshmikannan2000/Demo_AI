package framework.ai.agents.multi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import framework.ai.agents.OllamaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static framework.ai.prompts.SystemPrompts.TRIAGE_PROMPT;

public class TriageAgent {

    private static final Logger logger = LoggerFactory.getLogger(TriageAgent.class);

    private static final Set<String> CATEGORIES =
            Set.of("locator", "assertion", "timeout", "testdata", "environment", "unknown");

    public static void run(FailureContext ctx) {
        long start = System.currentTimeMillis();
        try {
            // User input as JSON (Gson escapes quotes/braces in the error text)
            JsonObject userInput = new JsonObject();
            userInput.addProperty("testName", ctx.getTestName());
            userInput.addProperty("errorMessage", ctx.getErrorMessage());

            //  Map.of — each message built in one line, no copy-paste of put() calls
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", TRIAGE_PROMPT));
            messages.add(Map.of("role", "user", "content", userInput.toString()));

            String reply = OllamaClient.chat(messages);
            long seconds = (System.currentTimeMillis() - start) / 1000;

            //check a JSON object exists before cutting it out
            int open = reply == null ? -1 : reply.indexOf('{');
            int close = reply == null ? -1 : reply.lastIndexOf('}');
            if (open == -1 || close < open) {
                throw new IllegalStateException("No JSON in triage reply: " + reply);
            }
            String json = reply.substring(open, close + 1);

            //Gson, same library as the rest of the AI code
            JsonObject result = JsonParser.parseString(json).getAsJsonObject();
            String category = result.get("category").getAsString().trim().toLowerCase();
            boolean needsCodeAnalysis = result.get("needsCodeAnalysis").getAsBoolean();
            String reason = result.has("reason") ? result.get("reason").getAsString() : "";

            if (!CATEGORIES.contains(category)) {
                logger.warn("[TRIAGE] Unknown category '{}' from model — using 'unknown'", category);
                category = "unknown";
            }

            ctx.setCategory(category);
            ctx.setNeedsCodeAnalysis(needsCodeAnalysis);
            ctx.setTriageReason(reason);
            ctx.addLog("TriageAgent → " + category
                    + ", needsCodeAnalysis=" + needsCodeAnalysis + " (" + seconds + " s)");

        } catch (Exception e) {
            // log WHY it failed, apply a safe fallback, record it in the ticket history
            logger.error("[TRIAGE] Failed — using fallback", e);
            ctx.setCategory("unknown");
            ctx.setNeedsCodeAnalysis(true);
            ctx.setTriageReason("Triage failed: " + e.getMessage());
            ctx.addLog("TriageAgent → FAILED, fallback: unknown, needsCodeAnalysis=true");
        }
    }

    // Test: one code-related failure, one environment failure
    public static void main(String[] args) {
        FailureContext ctxa = new FailureContext(
                "TC001_DEMO_Successful_Login",
                "Value is Not entered on Field expected [true] but found [false]",
                "DemoWebScripts.java");
        run(ctxa);
        logger.info("Test A:\n{}", ctxa.toJson());

        FailureContext ctxb = new FailureContext(
                "TC001_DEMO_Successful_Login",
                "java.net.ConnectException: Connection refused: connect",
                "DemoWebScripts.java");
        run(ctxb);
        logger.info("Test B:\n{}", ctxb.toJson());
    }
}

