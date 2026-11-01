package framework.ai.rag;


// One search result: which record, its code, which class it belongs to, and how close it was
public record RetrievedChunk (String id,String document,String className,Double distance){ }
