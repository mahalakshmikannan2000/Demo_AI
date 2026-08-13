package tests.mobile;

import framework.base.TestBase;
import framework.utils.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Demo Mobile suite — scaffold only, enabled = false until Appium ready.
 * executeFlag = N in TestNGXML.xlsx keeps these out of every run.
 * Mirrors your ECSScriptsAndroid / ECSScriptsIOS structure.
 *
 * Enable when:
 *   1. Appium server running locally OR BrowserStack credentials in .env
 *   2. APPIUM_APP_PATH set in .env pointing to a real .apk / .ipa
 *   3. Update executeFlag to Y in TestNGXML.xlsx MOBILE sheet
 */
public class DemoMobileScripts extends TestBase {

    @Test(description = "Android - Verify app launches successfully",
          dataProvider = "commonDataProvider",
          dataProviderClass = DataProvider.class,
          enabled = false)
    public void TC001_Android_AppLaunch(Map<String, String> data) {
        setTestData(data);
        // Add real Appium page object calls once driver is configured
    }

    @Test(description = "iOS - Verify app launches successfully",
          dataProvider = "commonDataProvider",
          dataProviderClass = DataProvider.class,
          enabled = false)
    public void TC001_IOS_AppLaunch(Map<String, String> data) {
        setTestData(data);
        // Add real Appium page object calls once driver is configured
    }
}
