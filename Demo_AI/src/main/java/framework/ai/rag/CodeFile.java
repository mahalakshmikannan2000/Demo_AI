package framework.ai.rag;

import java.nio.file.Path;

// One row of data per Java file: id + path + source travel together
public record CodeFile(String id, Path path, String source) {}