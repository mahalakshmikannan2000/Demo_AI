package framework.utils;

import com.aventstack.extentreports.Status;
import framework.ai.selfhealing.HealingUtils;
import framework.base.DriverFactory;
import framework.base.TestBase;
import framework.config.PropertyReader;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

/**
 * Common Selenium/Appium action helpers shared by the CSP web, ECS mobile and API page
 * objects. Trimmed from the full framework utility class down to only what the demo
 * test cases exercise.
 */
public class WebActions extends TestBase {

    public static final Logger logger = LoggerFactory.getLogger(WebActions.class);

    public static final int WAIT_5_SEC = 5;
    public static final int WAIT_10_SEC = 10;
    public static final int WAIT_15_SEC = 15;
    public static final int WAIT_20_SEC = 20;
    public static final int WAIT_30_SEC = 30;
    public static final int WAIT_40_SEC = 40;

    private static final ThreadLocal<Long> testDeadlineMs = new ThreadLocal<>();

    public static void setTestDeadline(long timeoutMs) {
        testDeadlineMs.set(System.currentTimeMillis() + timeoutMs);
    }

    public static void clearTestDeadline() {
        testDeadlineMs.remove();
    }

    /*
     * Method Description : Scrolls the page until the specified element is visible in the viewport
     * Input Parameter(s) if any : WebElement element
     * Output Parameter(s) if any : void
     */
    public static void scrollIntoElement(WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) DriverFactory.getDriver();
            js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
        } catch (JavascriptException e) {
            logger.error("JavaScript error while scrolling to element: {}", e.getMessage(), e);
        }
    }

    /*
     * Method Description : Waits until the specified element is visible
     * Input Parameter(s) if any : WebElement ele
     * Output Parameter(s) if any : boolean
     */
    public static boolean waitUntilElementVisible(WebElement ele) {
        try {
            Wait<WebDriver> wait = new FluentWait<>(DriverFactory.getDriver())
                    .withTimeout(Duration.ofSeconds(30))
                    .pollingEvery(Duration.ofMillis(400))
                    .ignoring(NoSuchElementException.class)
                    .ignoring(StaleElementReferenceException.class);
            wait.until(ExpectedConditions.visibilityOf(ele));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /*
     * Method Description : Waits until the specified element is clickable
     * Input Parameter(s) if any : WebElement ele
     * Output Parameter(s) if any : void
     */
    public static WebElement waitUntilElementClickable(WebElement ele,String tagName) {
        try {
            new WebDriverWait(DriverFactory.getDriver(), Duration.ofSeconds(WAIT_10_SEC))
                    .until(ExpectedConditions.elementToBeClickable(ele));
            return ele;
        } catch (Exception e) {
            logger.warn("Primary Locator is not found on page So going to Self Healing...");
           WebElement healedEle =  HealingUtils.tryHeal(DriverFactory.getDriver(),ele,tagName);
           if(healedEle!=null){
               logger.warn("Healed with new element and Continue with Testing..");
               return healedEle;
           }else{
               Assert.assertTrue(false,"Element is Not Clickable");
               return null;
           }
        }
    }

    public static void waitUntilElementClickable(WebElement ele) {
        try {
           new WebDriverWait(DriverFactory.getDriver(), Duration.ofSeconds(WAIT_10_SEC))
                    .until(ExpectedConditions.elementToBeClickable(ele));
        } catch (Exception e) {
            Assert.assertTrue(false,"Element is Not Clickable");
            }
        }
    /*
     * Method Description : Clicks on the element after scrolling into view (JS fallback on failure)
     * Input Parameter(s) if any : WebElement element
     * Output Parameter(s) if any : void
     */
//    public static void clickElement(WebElement element) {
//        try {
//            JavascriptExecutor js = (JavascriptExecutor) DriverFactory.getDriver();
//           WebElement actualElement =  waitUntilElementClickable(element,"button");
//            scrollIntoElement(actualElement);
//            try {
//                actualElement.click();
//            } catch (Exception e) {
//                js.executeScript("arguments[0].click();", actualElement);
//            }
//        } catch (Exception e) {
//            logger.error("Exception While click on Element {}", e.getMessage(), e);
//        }
//    }

    public static void clickElement(WebElement element) {
        try {
            waitUntilElementClickable(element);
            scrollIntoElement(element);
            element.click();
        } catch (Exception e) {
            logger.error("Exception While click on Element {}", e.getMessage(), e);
        }
    }

    /*
     * Method Description : Enters text into an element using sendKeys
     * Input Parameter(s) if any : WebElement element, String text
     * Output Parameter(s) if any : void
     */
//    public static void enterTextBySendKeys(WebElement element, String text) {
//       WebElement activeElement =  isElementPresent(element,"input");
////       isElementPresent(element,WAIT_5_SEC);
//       if(activeElement!=null){
//           scrollIntoElement(activeElement);
//           activeElement.clear();
//           activeElement.sendKeys(text);
//       }else{
//           Assert.assertTrue(false,"Value is Not entered on Field");
//       }
//    }

    /*
     * Method Description : Enters text into an element using sendKeys
     * Input Parameter(s) if any : WebElement element, String text
     * Output Parameter(s) if any : void
     */
    public static void enterTextBySendKeys(WebElement element, String text) {
        boolean isPresent =  isElementPresent(element,WAIT_5_SEC);
        if(isPresent){
            scrollIntoElement(element);
            element.clear();
            element.sendKeys(text);
        }else{
            Assert.assertTrue(false,"Value is Not entered on Field");
        }
    }



    /*
     * Method Description : Returns the visible text of an element, waiting until it is non-empty
     * Input Parameter(s) if any : WebElement elem
     * Output Parameter(s) if any : String
     */
    public static String getElementText(WebElement elem) {
        String text;
        do {
            text = elem.getText();
        } while (text.equals(""));
        return elem.getText();
    }

    /*
     * Method Description : Checks whether an element is visible within the default wait
     * Input Parameter(s) if any : WebElement element
     * Output Parameter(s) if any : boolean
     */
    public static WebElement isElementPresent( WebElement element,String tagname) {
        try {
            DriverFactory.getWait().until(ExpectedConditions.visibilityOf(element));
            return element;
        } catch (Exception e) {
        logger.warn("Primary Locator is not found on page So going to Self Healing...");
        WebElement healedEle =  HealingUtils.tryHeal(DriverFactory.getDriver(),element,tagname);
        if(healedEle!=null){
            logger.warn("Healed with new element and Continue with Testing..");
            return healedEle;
        }else{
            Assert.assertTrue(false,"Element is Not Clickable");
            return null;
        }
        }
    }

    /*
     * Method Description : Checks whether an element is visible within a custom timeout
     * Input Parameter(s) if any : WebElement element, int timeoutInSeconds
     * Output Parameter(s) if any : boolean
     */
    public static boolean isElementPresent(WebElement element, int timeoutInSeconds) {
        if (timeoutInSeconds <= 0) {
            throw new IllegalArgumentException("Timeout and polling interval must be greater than zero.");
        }
        Wait<WebDriver> customWait = new FluentWait<>(DriverFactory.getDriver())
                .withTimeout(Duration.ofSeconds(timeoutInSeconds))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
        try {
            customWait.until(ExpectedConditions.visibilityOf(element));
            return true;
        } catch (TimeoutException e) {
            logger.warn("Element not visible within {} seconds: {}", timeoutInSeconds, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Error while waiting for element visibility: {}", e.getMessage());
            return false;
        }
    }

    /*
     * Method Description : Waits for the given element to be present/visible (hard 30s wait)
     * Input Parameter(s) if any : WebElement element
     * Output Parameter(s) if any : void
     */
    public static void waitForElementToBePresent(WebElement element) {
        new WebDriverWait(DriverFactory.getDriver(), Duration.ofSeconds(WAIT_30_SEC)).until(ExpectedConditions.visibilityOf(element));
    }

    /*
     * Method Description : Refreshes the current browser page
     * Input Parameter(s) if any : none
     * Output Parameter(s) if any : void
     */
    public static void refreshPage() {
        DriverFactory.getDriver().navigate().refresh();
    }

    /*
     * Method Description : Clicks an element on mobile (Android/iOS) after waiting for visibility and clickability
     * Input Parameter(s) if any : WebElement element
     * Output Parameter(s) if any : void
     */
    public static void clickElementMobile(WebElement element) {
        WebDriverWait wait = new WebDriverWait(DriverFactory.getDriver(), Duration.ofSeconds(WAIT_30_SEC));
        wait.until(ExpectedConditions.visibilityOf(element));
        wait.until(ExpectedConditions.elementToBeClickable(element));
        element.click();
    }

    /*
     * Method Description : Sets text into an Android element (click + clear + sendKeys)
     * Input Parameter(s) if any : WebElement element, String text
     * Output Parameter(s) if any : void
     */
    public static void setTextAndroid(WebElement element, String text) {
        WebDriverWait wait = new WebDriverWait(DriverFactory.getDriver(), Duration.ofSeconds(WAIT_30_SEC));
        wait.until(ExpectedConditions.visibilityOf(element));
        element.click();
        element.clear();
        element.sendKeys(text);
    }

    /*
     * Method Description : Sets text into an iOS element using clipboard paste, with sendKeys fallback
     * Input Parameter(s) if any : WebElement element, String text
     * Output Parameter(s) if any : void
     */
    public static void setTextForIOS(WebElement element, String text) {
        element.click();
        try {
            ((IOSDriver) DriverFactory.getDriver()).setClipboardText(text);
            element.sendKeys(Keys.chord(Keys.COMMAND, "v"));
        } catch (Exception e) {
            logger.info("iOS clipboard paste unavailable, falling back to sendKeys");
        }
        element.clear();
        element.sendKeys(text);
    }

    /*
     * Method Description : Sets the implicit wait timeout on the driver
     * Input Parameter(s) if any : int seconds
     * Output Parameter(s) if any : void
     */
    public static void setImplicitWait(int seconds) {
        DriverFactory.getDriver().manage().timeouts().implicitlyWait(Duration.ofSeconds(seconds));
    }

    /*
     * Method Description : Logs an informational message in Extent report
     * Input Parameter(s) if any : String message
     * Output Parameter(s) if any : void
     */
    public static void logMessageInReport(String message) {
        if (ExtentReport.getDetailedTest() != null) {
            ExtentReport.getDetailedTest().log(Status.INFO, message);
        }
        if (ExtentReport.getEmailableTest() != null) {
            ExtentReport.getEmailableTest().log(Status.INFO, message);
        }
    }

    /*
     * Method Description : Logs a pass message in Extent report
     * Input Parameter(s) if any : String message
     * Output Parameter(s) if any : void
     */
    public static void logMessagePassInReport(String message) {
        if (ExtentReport.getDetailedTest() != null) {
            ExtentReport.getDetailedTest().log(Status.PASS, message);
        }
        if (ExtentReport.getEmailableTest() != null) {
            ExtentReport.getEmailableTest().log(Status.PASS, message);
        }
    }

    /*
     * Method Description : Logs a failure message in Extent report
     * Input Parameter(s) if any : String message
     * Output Parameter(s) if any : void
     */
    public static void logMessageFailInReport(String message) {
        if (ExtentReport.getDetailedTest() != null) {
            ExtentReport.getDetailedTest().log(Status.FAIL, message);
        }
        if (ExtentReport.getEmailableTest() != null) {
            ExtentReport.getEmailableTest().log(Status.FAIL, message);
        }
    }

    /*
     * Method Description : Sets the AES encryption/decryption key derived from a SHA-256 hash of the given text
     * Input Parameter(s) if any : String key
     * Output Parameter(s) if any : SecretKey
     */
    public static SecretKey setKey(String key) {
        MessageDigest sha;
        byte[] hashedKey = null;
        try {
            sha = MessageDigest.getInstance("SHA-256");
            hashedKey = sha.digest(key.getBytes());
        } catch (Exception e) {
            logger.error("Error in setting encrypt/decrypt key : ", e);
        }
        return new SecretKeySpec(hashedKey, "AES");
    }

    /*
     * Method Description : Decrypts a Base64-encoded, AES/GCM-encrypted string (username/password values
     * stored in the *.properties files) using the shared secretKey from config.properties
     * Input Parameter(s) if any : String bs
     * Output Parameter(s) if any : String
     */
    public static String dataDecrypt(String bs) {
        try {
            byte[] cipherText = Base64.getDecoder().decode(bs);
            SecretKey key = setKey(getRunTimeVariables("secretKey"));
            byte[] iv = new byte[12];
            System.arraycopy(cipherText, 0, iv, 0, 12);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] encryptedBytes = new byte[cipherText.length - 12];
            System.arraycopy(cipherText, 12, encryptedBytes, 0, encryptedBytes.length);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Error in decrypting data : ", e);
        }
        return null;
    }

    /*
     * Method Description : Retrieves a runtime variable from system property, environment variable,
     * demo.properties, then falls back to config.properties
     * Input Parameter(s) if any : String key
     * Output Parameter(s) if any : String
     */
    public static String getRunTimeVariables(String key) {
        String value = System.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        value = System.getenv(key);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        value = PropertyReader.getProperty("demo", key);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        value = PropertyReader.readProperty(key);
        if (value == null || value.trim().isEmpty()) {
            logger.error("Run Time variable is missing in config.properties: {}", key);
        }
        return value;
    }

    public static String getTagName(WebElement element){
        try{
            String tagName = element.getTagName();
            return tagName;
        } catch (Exception e) {
           logger.error("Exception While geting TagName of Element");
        }
        return null;
    }
}
