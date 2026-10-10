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
    public static final String EXAMPLE_INPUT_2 = """
        {"Test":"TC014_Checkout_Enter_Shipping_Details","Error":"Value is Not entered on Field expected [true] but found [false]","File":"CheckoutScripts.java","Framework":"Selenium Java TestNG"}

        Relevant source code (retrieved from codebase):
        package app.web;

        public class CheckoutPage {

            @FindBy(id = "first-name")
            private WebElement firstNameField;

            @FindBy(id = "zip-code_old")
            private WebElement zipCodeField;

            public void enterShippingDetails(String firstName, String zip) {
                enterTextBySendKeys(firstNameField, firstName);
                enterTextBySendKeys(zipCodeField, zip);
            }
        }
        """;

    public static final String EXAMPLE_OUTPUT_2 = """
        {
          "issueType": "locator",
          "title": "Zip code field locator is incorrect",
          "severity": "high",
          "rootCauseGuess": "The assertion failed because text could not be entered: the zipCodeField locator id 'zip-code_old' does not match the actual element, so the field is never found.",
          "suggestedFix": "Update @FindBy(id = \\"zip-code_old\\") in CheckoutPage to the current element id (likely \\"zip-code\\").",
          "fileToFix": "CheckoutPage.java",
          "isLikelyFlaky": false
        }
        """;
}
