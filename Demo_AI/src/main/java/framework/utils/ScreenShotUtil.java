package framework.utils;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import framework.base.DriverFactory;
import framework.base.TestBase;
import framework.config.PropertyReader;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

import static framework.utils.ExtentReport.TIMESTAMP;

/**
 * Captures screenshots (web + mobile, same TakesScreenshot API) and logs them into both
 * ExtentReports and Allure - demonstrating both reporting integrations from a single call.
 */
public class ScreenShotUtil {

    private ScreenShotUtil() {
        throw new UnsupportedOperationException("ScreenShotUtil class — do not instantiate.");
    }

    private static final Logger logger = LoggerFactory.getLogger(ScreenShotUtil.class);
    private static final String SCREEN_SHOT_FLAG = PropertyReader.readProperty("screenShotOnFailure");

    /*
     * Method Description : Captures a screenshot based on status, saves it under the report's
     * Screenshots folder and logs the step (with attachment) into both Extent reports
     * Input Parameter(s) if any : Status status, String message
     * Output Parameter(s) if any : void
     */
    public static void captureScreenshot(Status status, String message) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        String app = TestBase.appName;
        String screenshotPath = "reports" + File.separator + app + File.separator + TIMESTAMP + File.separator
                + "Screenshots" + File.separator + app + "_" + timestamp + ".png";
        String relativePath = "Screenshots" + File.separator + app + "_" + timestamp + ".png";
        File screenshotFile = null;

        if (shouldCapture(status)) {
            try {
                screenshotFile = ((TakesScreenshot) DriverFactory.getDriver()).getScreenshotAs(OutputType.FILE);
                File destination = new File(screenshotPath);
                destination.getParentFile().mkdirs();
                Files.copy(screenshotFile.toPath(), destination.toPath());
            } catch (IOException e) {
                logger.error("Error in taking Screenshot", e);
            }
        }

        try {
            ExtentTest detailedTest = ExtentReport.getDetailedTest();
            ExtentTest emailableTest = ExtentReport.getEmailableTest();
            String attachmentPath = screenshotFile != null ? relativePath : null;

            if (status.getName().equalsIgnoreCase("Pass")) {
                logOnPass(detailedTest, message, attachmentPath);
                logOnPass(emailableTest, message, null);
            } else if (status.getName().equalsIgnoreCase("Fail")) {
                logOnFail(detailedTest, message, attachmentPath, screenshotFile);
                logOnFail(emailableTest, message, null, null);
            } else if (status.getName().equalsIgnoreCase("Skip")) {
                logOnSkip(detailedTest, message, attachmentPath);
                logOnSkip(emailableTest, message, null);
            } else {
                logOnInfo(detailedTest, message, attachmentPath);
                logOnInfo(emailableTest, message, null);
            }
        } catch (Exception e) {
            logger.error("Error in logging test and taking Screenshot", e);
        }
    }

    private static boolean shouldCapture(Status status) {
        return (SCREEN_SHOT_FLAG.equalsIgnoreCase("true")
                && (status.getName().equalsIgnoreCase("fail") || status.getName().equalsIgnoreCase("skip")))
                || SCREEN_SHOT_FLAG.equalsIgnoreCase("false");
    }

    /*
     * Method Description : Logs a passed step into the given Extent test, optionally with a screenshot
     * Input Parameter(s) if any : ExtentTest test, String message, String relativePath
     * Output Parameter(s) if any : void
     */
    public static void logOnPass(ExtentTest test, String message, String relativePath) {
        if (test != null) {
            if (SCREEN_SHOT_FLAG.equalsIgnoreCase("false") && relativePath != null) {
                test.pass(message, MediaEntityBuilder.createScreenCaptureFromPath(relativePath).build());
            } else {
                test.pass(message);
            }
        }
    }

    /*
     * Method Description : Logs a failed step into the given Extent test and attaches the screenshot
     * to the Allure report lifecycle as well
     * Input Parameter(s) if any : ExtentTest test, String message, String relativePath, File screenshotFile
     * Output Parameter(s) if any : void
     * Throws : FileNotFoundException if the screenshot file cannot be located
     */
    public static void logOnFail(ExtentTest test, String message, String relativePath, File screenshotFile) throws FileNotFoundException {
        if (test != null) {
            if (relativePath != null && screenshotFile != null) {
                test.fail(message, MediaEntityBuilder.createScreenCaptureFromPath(relativePath).build());
                Allure.getLifecycle().addAttachment(message, "image/png", "png", new FileInputStream(screenshotFile));
            } else {
                test.fail(message);
            }
        } else {
            logger.error("ExtentTest is null while capturing and logging screenshot.");
        }
    }

    /*
     * Method Description : Logs a skipped step into the given Extent test, optionally with a screenshot
     * Input Parameter(s) if any : ExtentTest test, String message, String relativePath
     * Output Parameter(s) if any : void
     */
    public static void logOnSkip(ExtentTest test, String message, String relativePath) {
        if (test != null) {
            if (relativePath != null) {
                test.skip(message, MediaEntityBuilder.createScreenCaptureFromPath(relativePath).build());
            } else {
                test.skip(message);
            }
        }
    }

    /*
     * Method Description : Logs an informational step into the given Extent test, optionally with a screenshot
     * Input Parameter(s) if any : ExtentTest test, String message, String relativePath
     * Output Parameter(s) if any : void
     */
    public static void logOnInfo(ExtentTest test, String message, String relativePath) {
        if (test != null) {
            if (SCREEN_SHOT_FLAG.equalsIgnoreCase("false") && relativePath != null) {
                test.info(message, MediaEntityBuilder.createScreenCaptureFromPath(relativePath).build());
            } else {
                test.info(message);
            }
        }
    }
}
