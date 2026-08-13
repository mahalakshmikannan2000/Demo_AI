package framework.base;

import framework.utils.AllureReportGenerator;
import framework.utils.ExtentReport;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

import java.lang.reflect.Method;
import java.util.Map;

import static framework.base.ObjectInitiatorFactory.apiObjectInitiator;
import static framework.base.ObjectInitiatorFactory.objectInitiator;
import static framework.config.PropertyReader.getPropertyFileURL;
import static framework.utils.WebActions.logMessageFailInReport;

/**
 * Base class for every test class: resolves environment/app config, boots ExtentReports
 * before the suite, initializes the right WebDriver/page-object set before each test,
 * and tears everything down (driver quit, Extent flush, Allure report generation) afterwards.
 */
public class TestBase {

    private static String url;
    public static String appName;
    public static String appEnv;
    private static final ThreadLocal<String> browserName = new ThreadLocal<>();
    private static final Logger logger = LoggerFactory.getLogger(TestBase.class);
    private static final ThreadLocal<Long> startTime = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, String>> testDataThreadLocal = new ThreadLocal<>();

    public static void setBrowserName(String b) {
        browserName.set(b);
    }

    public static String getBrowserName() {
        return browserName.get();
    }

    public static void removeBrowserName() {
        browserName.remove();
    }

    public static String getUrl() {
        return url;
    }

    /*
     * Method Description : Sets up ExtentReports and resolves the application URL before the suite runs
     * Input Parameter(s)if any: env (String) - Environment name in format "app_env" (e.g. CSP_QA)
     * Output Parameter(s)(if any): None
     */
    @BeforeSuite(alwaysRun = true)
    @Parameters("env")
    public static void setupReport(String env) {
        appEnv = env;
        String[] environment = env.split("_");
        appName = environment[0];
        if (!"API".equalsIgnoreCase(appName)) {
            url = getPropertyFileURL(environment[0], environment[1]);
        }
        ExtentReport.createInstances(appName);
        logger.info("Before Suite: environment={}, app={}", env, appName);
    }

    /*
     * Method Description : Initializes the WebDriver (or REST client) and page objects before each test method
     * Input Parameter(s)if any: method (Method), context (ITestContext)
     * Output Parameter(s)(if any): None
     */
    @BeforeMethod
    public void beforeMethod(Method method, ITestContext context) {
        try {
            String browser = context.getCurrentXmlTest().getParameter("browser").toLowerCase();
            setBrowserName(browser);
            startTime.set(System.currentTimeMillis());
            switch (browser) {
                case "chrome", "edge" -> {
                    DriverFactory.setDriver(browser);
                    WebDriver driver = DriverFactory.getDriver();
                    objectInitiator(driver, appName);
                    driver.manage().window().maximize();
                    driver.get(getUrl());
                }
                case "android_app", "ios_app" -> {
                    DriverFactory.setDriver(browser);
                    WebDriver driver = DriverFactory.getDriver();
                    objectInitiator(driver, appName);
                }
                case "api" -> apiObjectInitiator(appName);
                default -> throw new IllegalArgumentException("Unsupported browser: " + browser);
            }
        } catch (Exception e) {
            logMessageFailInReport("Could not open the browser" + e);
        }
    }

    /*
     * Method Description : Quits the driver (if any), calculates execution time and clears thread-local state
     * Input Parameter(s)if any: result (ITestResult), method (Method), context (ITestContext)
     * Output Parameter(s)(if any): None
     */
    @AfterMethod(alwaysRun = true)
    public void afterMethod(ITestResult result, Method method, ITestContext context) {
        String browser = getBrowserName() == null ? "" : getBrowserName().toLowerCase();
        boolean quitDriverNeeded = switch (browser) {
            case "chrome", "edge", "android_app", "ios_app" -> true;
            default -> false;
        };
        if (quitDriverNeeded) {
            try {
                DriverFactory.quitDriver();
            } catch (Exception e) {
                logger.error("Error during driver quit", e);
            }
        }
        calculateExecutionTime(method);
        removeBrowserName();
        clearTestData();
        framework.utils.WebActions.clearTestDeadline();
    }

    /*
     * Method Description : Stores test data in ThreadLocal storage for the current test thread
     * Input Parameter(s)if any: data (Map<String, String>)
     * Output Parameter(s)(if any): None
     */
    public static void setTestData(Map<String, String> data) {
        testDataThreadLocal.set(data);
    }

    /*
     * Method Description : Retrieves test data stored in ThreadLocal for the current test thread
     * Input Parameter(s)if any: None
     * Output Parameter(s)(if any): Map<String, String>
     */
    public static Map<String, String> getTestData() {
        return testDataThreadLocal.get();
    }

    /*
     * Method Description : Clears test data from ThreadLocal for the current test thread
     * Input Parameter(s)if any: None
     * Output Parameter(s)(if any): None
     */
    public static void clearTestData() {
        testDataThreadLocal.remove();
    }

    private void calculateExecutionTime(Method method) {
        Long start = startTime.get();
        if (start == null) {
            return;
        }
        long duration = System.currentTimeMillis() - start;
        logger.info("{} - Test Execution Time : {} ms", method.getName(), duration);
        startTime.remove();
    }

    /*
     * Method Description : Flushes Extent reports and generates the Allure report after the suite completes
     * Input Parameter(s)if any: context (ITestContext)
     * Output Parameter(s)(if any): None
     */
    @AfterSuite(alwaysRun = true)
    public static void tearDownSuite(ITestContext context) {
        try {
            ExtentReport.flushReports();
            boolean pipelineHandlesAllure = Boolean.parseBoolean(System.getProperty("pipelineAllureHandled", "false"));
            if (!pipelineHandlesAllure) {
                AllureReportGenerator.generateAllureReport();
            }
            logger.info("Suite finished. Extent report: {} | Allure results: {}",
                    ExtentReport.getEmailableReportPath(), "allure-results");
        } catch (Exception e) {
            logger.error("Error during suite teardown", e);
        }
    }
}
