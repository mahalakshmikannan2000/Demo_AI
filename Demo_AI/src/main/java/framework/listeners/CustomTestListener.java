package framework.listeners;

import com.aventstack.extentreports.Status;
import framework.ai.agents.FailureAnalysisService;
import framework.base.DriverFactory;
import framework.base.TestBase;
import framework.utils.ExtentReport;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static framework.base.TestBase.getBrowserName;
import static framework.utils.ScreenShotUtil.captureScreenshot;

/**
 * TestNG listener wiring test lifecycle events into ExtentReports (create test / pass / fail /
 * skip / execution time / screenshots) and, for mobile runs, marking the BrowserStack session
 * status. This is the "reporting glue" behind the Extent + Allure feature highlighted in the demo.
 */
public class CustomTestListener implements ITestListener, ISuiteListener {

    private static final ThreadLocal<Long> startTime = new ThreadLocal<>();
    private static final Logger logger = LoggerFactory.getLogger(CustomTestListener.class);
    private static final String KEY_BROWSER = "browser";
    public static LocalDateTime suiteStartTime;
    public static LocalDateTime suiteEndTime;

    @Override
    public void onStart(ISuite suite) {
        logger.info("Suite started: {}", suite.getName());
        suiteStartTime = LocalDateTime.now();
    }

    @Override
    public void onFinish(ISuite suite) {
        suiteEndTime = LocalDateTime.now();
        logger.info("Suite finished: {}", suite.getName());
        String aiAnalysis = FailureAnalysisService.analyseAndBuildMessages();
        logger.info("[AI] Failure Analysis Complete: {}", aiAnalysis);
    }

    /*
     * Method Description : Creates the detailed + emailable Extent tests when a test method starts
     * Input Parameter(s) if any : ITestResult result
     * Output Parameter(s) if any : void
     */
    @Override
    public void onTestStart(ITestResult result) {
        startTime.set(System.currentTimeMillis());
        ExtentReport.setDetailedTest(ExtentReport.getDetailedExtent().createTest(result.getMethod().getMethodName()));
        ExtentReport.getDetailedTest().assignCategory("Browser: " + getBrowserName());
        ExtentReport.setEmailableTest(ExtentReport.getEmailableExtent().createTest(result.getMethod().getMethodName()));
        ExtentReport.getEmailableTest().assignCategory("Browser: " + getBrowserName());
        logger.info("Test started: {}", result.getMethod().getMethodName());
    }

    /*
     * Method Description : Logs a pass in Extent reports and marks the BrowserStack session (mobile only)
     * Input Parameter(s) if any : ITestResult result
     * Output Parameter(s) if any : void
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentReport.getDetailedTest().pass("Test Passed");
        ExtentReport.getEmailableTest().pass("Test Passed");
        logger.info("Test passed: {}", result.getMethod().getMethodName());

        logExecutionTime(result);
        markBrowserStackSession(result);
        TestBase.clearTestData();
        removeAllTests();
    }

    /*
     * Method Description : Logs a failure, captures a screenshot, and marks the BrowserStack session (mobile only)
     * Input Parameter(s) if any : ITestResult result
     * Output Parameter(s) if any : void
     */
    @Override
    public void onTestFailure(ITestResult result) {
        Throwable throwable = result.getThrowable();
        ExtentReport.getDetailedTest().fail("Test Failed: " + throwable);
        ExtentReport.getEmailableTest().fail("Test Failed: " + throwable);
        logger.error("Test failed: {}", result.getMethod().getMethodName(), throwable);

        if (DriverFactory.getDriver() != null && throwable != null) {
            captureScreenshot(Status.FAIL, throwable.getMessage());
        }

        logExecutionTime(result);
        markBrowserStackSession(result);
        TestBase.clearTestData();
        removeAllTests();
    }

    /*
     * Method Description : Logs a skip, captures a screenshot, and marks the BrowserStack session (mobile only)
     * Input Parameter(s) if any : ITestResult result
     * Output Parameter(s) if any : void
     */
    @Override
    public void onTestSkipped(ITestResult result) {
        Throwable skipReason = result.getThrowable();
        logger.warn("Test skipped: {}", result.getMethod().getMethodName());

        ExtentReport.getDetailedTest().skip("Test Skipped" + (skipReason != null ? ": " + skipReason : ""));
        ExtentReport.getEmailableTest().skip("Test Skipped" + (skipReason != null ? ": " + skipReason : ""));

        if (DriverFactory.getDriver() != null && skipReason != null) {
            captureScreenshot(Status.SKIP, Objects.requireNonNullElse(skipReason.getMessage(), "Test skipped"));
        }

        logExecutionTime(result);
        markBrowserStackSession(result);
        TestBase.clearTestData();
        removeAllTests();
    }

    private void logExecutionTime(ITestResult result) {
        Long start = startTime.get();
        if (start == null) {
            return;
        }
        long duration = System.currentTimeMillis() - start;
        long seconds = (duration / 1000) % 60;
        long minutes = (duration / (1000 * 60)) % 60;
        long hours = (duration / (1000 * 60 * 60));

        String time = String.format("%02dh:%02dm:%02ds", hours, minutes, seconds);
        ExtentReport.getDetailedTest().log(Status.INFO, "Execution Time: " + time);
        ExtentReport.getEmailableTest().log(Status.INFO, "Execution Time: " + time);
        logger.info("Execution time for {}: {}", result.getMethod().getMethodName(), time);
        startTime.remove();
    }

    public static void removeAllTests() {
        ExtentReport.removeDetailedTest();
        ExtentReport.removeEmailableTest();
    }

    public static String formatMillis(long millis) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
        return formatter.format(Instant.ofEpochMilli(millis));
    }

    /*
     * Method Description : Sets the BrowserStack session status/reason via the JS executor (mobile app runs only)
     * Input Parameter(s) if any : ITestResult result
     * Output Parameter(s) if any : void
     */
    private void markBrowserStackSession(ITestResult result) {
        String browser = getBrowserName();
        if (browser == null || !(browser.equalsIgnoreCase("android_app") || browser.equalsIgnoreCase("ios_app"))) {
            return;
        }
        try {
            WebDriver driver = DriverFactory.getDriver();
            if (driver == null) return;

            JavascriptExecutor jse = (JavascriptExecutor) driver;
            String status;
            String reason;
            switch (result.getStatus()) {
                case ITestResult.SUCCESS -> { status = "passed"; reason = "Test passed"; }
                case ITestResult.FAILURE -> { status = "failed"; reason = "Test failed: " + result.getThrowable(); }
                case ITestResult.SKIP -> { status = "skipped"; reason = "Test skipped"; }
                default -> { status = "unknown"; reason = "Unknown status"; }
            }

            jse.executeScript("browserstack_executor: {\"action\": \"setSessionStatus\", "
                    + "\"arguments\": {\"status\":\"" + status + "\", \"reason\": \"" + reason.replace("\"", "'") + "\"}}");
            logger.info("BrowserStack session status set to: {}", status);
        } catch (Exception e) {
            logger.warn("Failed to set BrowserStack session status: {}", e.getMessage());
        }
    }
}
