package framework.ai.agents;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.output.Response;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class OllamaClient {

    private static final Logger logger = LoggerFactory.getLogger(OllamaClient.class);
    private static String ollamaBaseUrl;
    private static String ollamaModel;

     static {
        Dotenv dotenv = Dotenv.load();
        ollamaBaseUrl = dotenv.get("OLLAMA_BASE_URL");
        ollamaModel = dotenv.get("OLLAMA_MODEL");
    }



    public  static String chat(List<Map<String,String>> messages) {
        String result = null;
        try {
            List<ChatMessage> chatMessage = new ArrayList<>();
            OllamaChatModel modelbuild = OllamaChatModel.builder().baseUrl(ollamaBaseUrl).modelName(ollamaModel).timeout(Duration.ofSeconds(5)).build();

            for (Map<String, String> message : messages) {
                String role = message.get("role");
                String content = message.get("content");
                switch (role) {
                    case "system" -> chatMessage.add(SystemMessage.from(content));
                    case "user" -> chatMessage.add(UserMessage.from(content));
                    case "assistant" -> chatMessage.add(AiMessage.from(content));
                }
            }
            logger.info("[OLLAMA] Sending {} messages to model: {}", chatMessage.size(), ollamaModel);
            Response<AiMessage> response = modelbuild.generate(chatMessage);

            result = response.content().text();
//            logger.info("[OLLAMA] Response received: {}", result);

        } catch (Exception e) {
            logger.error("Exception while creating [OLLAMA] Response received: {}", e.getMessage(), e);
        }
        return result;
    }



}
