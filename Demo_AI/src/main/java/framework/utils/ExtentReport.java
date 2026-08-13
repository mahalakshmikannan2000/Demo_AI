package framework.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Wires up two ExtentReports instances (a full "detailed" report and a lightweight
 * "emailable" summary) per suite run - the Extent Reports capability highlighted in
 * this demo.
 */
public class ExtentReport {

    private static final ThreadLocal<ExtentTest> detailedTestThread = new ThreadLocal<>();
    private static final ThreadLocal<ExtentTest> emailableTestThread = new ThreadLocal<>();

    private static ExtentReports detailedExtent;
    private static ExtentReports emailableExtent;

    public static final String TIMESTAMP = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

    private static String emailableReportPath;

    private ExtentReport() {
        throw new UnsupportedOperationException("Extent Report class — do not instantiate.");
    }

    public static String getEmailableReportPath() {
        return emailableReportPath;
    }

    /*
     * Method Description : Sets the current detailed ExtentTest in ThreadLocal storage
     * Input Parameter(s) if any : ExtentTest test
     * Output Parameter(s) if any : void
     */
    public static void setDetailedTest(ExtentTest test) {
        detailedTestThread.set(test);
    }

    public static ExtentTest getDetailedTest() {
        return detailedTestThread.get();
    }

    public static void removeDetailedTest() {
        detailedTestThread.remove();
    }

    /*
     * Method Description : Sets the current emailable ExtentTest in ThreadLocal storage
     * Input Parameter(s) if any : ExtentTest test
     * Output Parameter(s) if any : void
     */
    public static void setEmailableTest(ExtentTest test) {
        emailableTestThread.set(test);
    }

    public static ExtentTest getEmailableTest() {
        return emailableTestThread.get();
    }

    public static void removeEmailableTest() {
        emailableTestThread.remove();
    }

    /*
     * Method Description : Creates and initializes the detailed + emailable ExtentReports instances
     * Input Parameter(s) if any : String app - application name used in the report folder path
     * Output Parameter(s) if any : void
     */
    public static void createInstances(String app) {
        String reportFolderPath = "reports" + File.separator + app + File.separator + TIMESTAMP + File.separator;
        String screenshotFolderPath = reportFolderPath + "Screenshots" + File.separator;

        new File(reportFolderPath).mkdirs();
        new File(screenshotFolderPath).mkdirs();

        emailableReportPath = reportFolderPath + app + "_Emailable_Report.html";
        ExtentSparkReporter emailableReporter = new ExtentSparkReporter(emailableReportPath);
        emailableReporter.config().setReportName("Emailable Test Summary");
        emailableReporter.config().setDocumentTitle("Emailable Automation Report");
        emailableReporter.config().setTheme(Theme.DARK);

        String detailedReportPath = reportFolderPath + app + "_Main_Report_" + TIMESTAMP + ".html";
        ExtentSparkReporter detailedReporter = new ExtentSparkReporter(detailedReportPath);
        detailedReporter.config().setReportName("Automation Test Report");
        detailedReporter.config().setDocumentTitle("Extent Report - Dark Theme");
        detailedReporter.config().setTheme(Theme.DARK);

        detailedExtent = new ExtentReports();
        detailedExtent.attachReporter(detailedReporter);

        emailableExtent = new ExtentReports();
        emailableExtent.attachReporter(emailableReporter);
    }

    public static ExtentReports getDetailedExtent() {
        return detailedExtent;
    }

    public static ExtentReports getEmailableExtent() {
        return emailableExtent;
    }

    /*
     * Method Description : Flushes both ExtentReports instances so the HTML files are written to disk
     * Input Parameter(s) if any : None
     * Output Parameter(s) if any : void
     */
    public static void flushReports() {
        if (detailedExtent != null) detailedExtent.flush();
        if (emailableExtent != null) emailableExtent.flush();
    }
}
