package framework.ai.prompts;

public class FewShotExamples {

    private FewShotExamples(){}

    public static final String EXAMPLE_INPUT_1 = """
       
        {
            "Test": "TC001_DEMO_Successful_Login",
            "Error": "NoSuchElementException — unable to locate element id=login-button-Broken",
            "File":" DemoLoginPage.java",
            "Framework": "Selenium Java TestNG"
        }
        """;
    public static final String EXAMPLE_OUTPUT_1 = """
        {
          "issueType": "locator",
          "title": "Locator Missing Issue on Login Page",
          "severity": "high",
          "rootCauseGuess": "Element is not loaded on the page/Element is missing on DOM",
          "suggestedFix": "Check the locator of Element on page",
          "fileToFix": "DemoLoginPage.java",
          "isLikelyFlaky": true
        }
        """;
}
