package framework.ai.rag;
import java.nio.file.Path;

public record CodeFile(String id, Path path, String source){ }