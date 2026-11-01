package framework.ai.agents.multi;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

public class FailureContext {

    //  Allure Inputs
    private String testName;
    private String errorMessage;
    private String allureFileToFix;

    // TriageAgent Fields
    private String category;
    private String triageReason;
    private boolean needsCodeAnalysis;

    // CodeAnalysisAgent Fields
    private String retrievedCode;
    private String fileToFix;
    private String faultyLine;
    private String analysisReason;
    private List<String> retrievedIds;

    // FixSuggestionAgent Fields
    private String suggestedFix;
    private String confidence;

    // Human Review
    private ReviewStatus reviewStatus;

    // Agent Trace
    private List<String> agentLog;

     public FailureContext (String testName, String errorMessage,String allureFileToFix){
        this.testName = testName;
        this.errorMessage = errorMessage;
        this.allureFileToFix = allureFileToFix;

        this.retrievedIds = new ArrayList<>();
        this.agentLog = new ArrayList<>();

        this.reviewStatus = ReviewStatus.NOT_REQUIRED;
    }

    public String getTestName() {
        return testName;
    }


    public String getErrorMessage() {
        return errorMessage;
    }


    public String getAllureFileToFix() {
        return allureFileToFix;
    }


    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTriageReason() {
        return triageReason;
    }

    public void setTriageReason(String triageReason) {
        this.triageReason = triageReason;
    }

    public boolean isNeedsCodeAnalysis() {
        return needsCodeAnalysis;
    }

    public void setNeedsCodeAnalysis(boolean needsCodeAnalysis) {
        this.needsCodeAnalysis = needsCodeAnalysis;
    }

    public String getRetrievedCode() {
        return retrievedCode;
    }

    public void setRetrievedCode(String retrievedCode) {
        this.retrievedCode = retrievedCode;
    }

    public String getFileToFix() {
        return fileToFix;
    }

    public void setFileToFix(String fileToFix) {
        this.fileToFix = fileToFix;
    }

    public String getFaultyLine() {
        return faultyLine;
    }

    public void setFaultyLine(String faultyLine) {
        this.faultyLine = faultyLine;
    }

    public String getAnalysisReason() {
        return analysisReason;
    }

    public void setAnalysisReason(String analysisReason) {
        this.analysisReason = analysisReason;
    }

    public List<String> getRetrievedIds() {
        return retrievedIds;
    }

    public void setRetrievedIds(List<String> retrievedIds) {
        this.retrievedIds = retrievedIds;
    }

    public String getSuggestedFix() {
        return suggestedFix;
    }

    public void setSuggestedFix(String suggestedFix) {
        this.suggestedFix = suggestedFix;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(ReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public List<String> getAgentLog() {
        return agentLog;
    }

    public void addLog(String entry){
        agentLog.add(entry);
    }

    public String toJson(){
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(this);
    }

    public static void main(String[] args){
        FailureContext context =  new FailureContext("TC001_DEMO_Successful_Login","Value is Not entered on Field expected [true] but found [false]","DemoWebScripts.java");
        context.setCategory("locator");
        context.setNeedsCodeAnalysis(true);
        context.addLog("TriageAgent → locator");
        System.out.println(context.toJson());
    }
}
