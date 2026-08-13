package framework.base;

import framework.config.PropertyReader;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static framework.base.TestBase.appEnv;
import static framework.utils.WebActions.getRunTimeVariables;
import static framework.utils.WebActions.logMessageInReport;

/**
 * Creates and tears down WebDriver/AppiumDriver instances for Chrome/Edge (local via
 * WebDriverManager) and Android/iOS apps (remote via BrowserStack) - the "browser config"
 * capability highlighted in this demo.
 */
public class DriverFactory {

    private static final ThreadLocal<WebDriverWait> wait = new ThreadLocal<>();
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    private static final Logger logger = LoggerFactory.getLogger(DriverFactory.class);
    private static final String CONFIG = "config";
    private static final String DEVICE_NAME = "deviceName";
    private static final long QUIT_TIMEOUT_SECONDS = 20;

    private DriverFactory() {
        throw new UnsupportedOperationException("Driver class — do not instantiate.");
    }

    /*
     * Method Description : Initializes WebDriver/AppiumDriver instance based on browser/device type
     * Input Parameter(s)if any: strBrowserType (String) - Browser/device type
     * Output Parameter(s)(if any): None (sets WebDriver & WebDriverWait in ThreadLocal)
     */
    public static void setDriver(String strBrowserType) {
        try {
            String user = getRunTimeVariables("bs_user");
            String key = getRunTimeVariables("bs_key");
            String url = "https://" + user + ":" + key + "@hub-cloud.browserstack.com/wd/hub";

            switch (strBrowserType) {
                case "chrome" -> setChromeDriver();
                case "edge" -> setEdgeDriver();
                case "android_app" -> setAndroidAppDriver(url, user, key);
                case "ios_app" -> setIosAppDriver(url, user, key);
                default -> logger.warn("Unsupported browser type requested: {}", strBrowserType);
            }
        } catch (Exception e) {
            Assert.fail("Could not open the browser" + e);
        }
    }

    private static void setChromeDriver() {
        ChromeOptions chromeOptions = new ChromeOptions();
        setChromeOptions(chromeOptions);
        if (PropertyReader.readProperty("browserSource").equalsIgnoreCase("driverManager")) {
            WebDriverManager.chromedriver().setup();
            WebDriver driver = new ChromeDriver(chromeOptions);
            driverThreadLocal.set(driver);
            wait.set(new WebDriverWait(driver, Duration.ofSeconds(30)));
        } else {
            System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir") + "/driver/chromedriver.exe");
            WebDriver driver = new ChromeDriver(chromeOptions);
            driverThreadLocal.set(driver);
            wait.set(new WebDriverWait(driver, Duration.ofSeconds(15)));
        }
    }

    private static void setEdgeDriver() {
        EdgeOptions edgeOptions = new EdgeOptions();
        setEdgeOptions(edgeOptions);
        if (PropertyReader.readProperty("browserSource").equalsIgnoreCase("driverManager")) {
            WebDriverManager.edgedriver().setup();
            WebDriver driver = new EdgeDriver(edgeOptions);
            driver.manage().window().maximize();
            driverThreadLocal.set(driver);
            wait.set(new WebDriverWait(driver, Duration.ofSeconds(30)));
        } else {
            System.setProperty("webdriver.edge.driver", System.getProperty("user.dir") + "/driver/msedgedriver.exe");
            WebDriver driver = new EdgeDriver(edgeOptions);
            driver.manage().window().maximize();
            driverThreadLocal.set(driver);
            wait.set(new WebDriverWait(driver, Duration.ofSeconds(15)));
        }
    }

    private static void setAndroidAppDriver(String url, String user, String key) {
        try {
            String appUrl = appEnv.contains("QA") ? getRunTimeVariables("QA_android_app_url") : getRunTimeVariables("TEST_android_app_url");
            DesiredCapabilities caps = new DesiredCapabilities();
            caps.setCapability("platformName", "Android");
            caps.setCapability(DEVICE_NAME, PropertyReader.getProperty(CONFIG, "android_device"));
            caps.setCapability("os_version", PropertyReader.getProperty(CONFIG, "android_os_version"));
            caps.setCapability("unicodeKeyboard", true);
            caps.setCapability("resetKeyboard", true);
            caps.setCapability("noReset", true);
            caps.setCapability("fullReset", false);
            caps.setCapability("app", appUrl.trim());
            caps.setCapability("project", "CSP Login Demo");
            caps.setCapability("build", "Demo Build");
            caps.setCapability("name", "Android Login Demo");

            driverThreadLocal.set(new AndroidDriver(new URL(url), caps));
            wait.set(new WebDriverWait(driverThreadLocal.get(), Duration.ofSeconds(20)));
        } catch (Exception e) {
            logMessageInReport("Failed to initialize BrowserStack Android driver: " + e.getMessage());
        }
    }

    private static void setIosAppDriver(String url, String user, String key) {
        try {
            String appUrl = appEnv.contains("QA") ? getRunTimeVariables("QA_ios_app_url") : getRunTimeVariables("TEST_ios_app_url");
            DesiredCapabilities caps = new DesiredCapabilities();
            caps.setCapability("browserstack.user", user);
            caps.setCapability("browserstack.key", key);
            caps.setCapability("platformName", "iOS");
            caps.setCapability(DEVICE_NAME, PropertyReader.getProperty(CONFIG, "ios_device"));
            caps.setCapability("os_version", PropertyReader.getProperty(CONFIG, "ios_os_version"));
            caps.setCapability("noReset", true);
            caps.setCapability("fullReset", false);
            caps.setCapability("app", appUrl.trim());
            caps.setCapability("project", "CSP Login Demo");
            caps.setCapability("build", "Demo Build");
            caps.setCapability("name", "iOS Login Demo");

            driverThreadLocal.set(new IOSDriver(new URL(url), caps));
            wait.set(new WebDriverWait(driverThreadLocal.get(), Duration.ofSeconds(30)));
        } catch (Exception e) {
            logMessageInReport("Failed to initialize BrowserStack iOS driver: " + e.getMessage());
        }
    }

    /*
     * Method Description : Sets Chrome-specific options such as headless mode, sandbox, GPU, and certificate handling
     * Input Parameter(s)if any: chromeOptions (ChromeOptions) - Chrome options instance to configure
     * Output Parameter(s)(if any): None
     */
    public static void setChromeOptions(ChromeOptions chromeOptions) {
        if (getRunTimeVariables("headless").equalsIgnoreCase("true")) {
            chromeOptions.addArguments("--headless=new");
            chromeOptions.addArguments("--disable-gpu");
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-dev-shm-usage");
            chromeOptions.addArguments("--remote-allow-origins=*");
            chromeOptions.addArguments("--ignore-certificate-errors");
            chromeOptions.addArguments("--window-size=1920,1080");
        } else {
            chromeOptions.addArguments("--start-maximized");
            chromeOptions.addArguments("--window-size=1920,1080");
        }
        chromeOptions.setPageLoadStrategy(PageLoadStrategy.NORMAL);

        String downloadPath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + "TestFiles";
        File downloadDir = new File(downloadPath);
        if (!downloadDir.exists()) downloadDir.mkdirs();

        HashMap<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir.getAbsolutePath());
        prefs.put("download.prompt_for_download", false);
        chromeOptions.setExperimentalOption("prefs", prefs);
    }

    /*
     * Method Description : Sets Edge-specific options such as headless mode, sandbox, GPU, and certificate handling
     * Input Parameter(s)if any: edgeOptions (EdgeOptions) - Edge options instance to configure
     * Output Parameter(s)(if any): None
     */
    public static void setEdgeOptions(EdgeOptions edgeOptions) {
        if (getRunTimeVariables("headless").equalsIgnoreCase("true")) {
            edgeOptions.addArguments("--headless");
        }
        edgeOptions.addArguments("--no-sandbox");
        edgeOptions.addArguments("--disable-gpu");
        edgeOptions.addArguments("--remote-allow-origins=*");
        edgeOptions.addArguments("--ignore-certificate-errors");
        edgeOptions.addArguments("--window-size=1920x1080");
        edgeOptions.setPageLoadStrategy(PageLoadStrategy.NORMAL);
    }

    /*
     * Method Description : Returns the current thread's WebDriver instance
     * Input Parameter(s)if any: None
     * Output Parameter(s)(if any): WebDriver
     */
    public static WebDriver getDriver() {
        return driverThreadLocal.get();
    }

    /*
     * Method Description : Quits the current thread's WebDriver instance with a hard timeout, then clears ThreadLocal
     * Input Parameter(s)if any: None
     * Output Parameter(s)(if any): None
     */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                quitWithTimeout(driver);
            } finally {
                driverThreadLocal.remove();
                wait.remove();
            }
        }
    }

    private static void quitWithTimeout(WebDriver driver) {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DriverQuitWorker");
            t.setDaemon(true);
            return t;
        });
        Future<?> quitTask = executor.submit(driver::quit);
        try {
            quitTask.get(QUIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.error("driver.quit() did not complete within {} seconds. Abandoning it.", QUIT_TIMEOUT_SECONDS);
            quitTask.cancel(true);
        } catch (Exception e) {
            logger.error("Error while quitting driver: {}", e.getMessage(), e);
        } finally {
            executor.shutdownNow();
        }
    }

    /*
     * Method Description : Returns the current thread's WebDriverWait instance
     * Input Parameter(s)if any: None
     * Output Parameter(s)(if any): WebDriverWait
     */
    public static WebDriverWait getWait() {
        return wait.get();
    }
}
