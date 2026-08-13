package tests.web;

import framework.base.PageObjectInitiatorDemo;
import framework.base.TestBase;
import framework.utils.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Demo web suite — targets saucedemo.com
 * Mirrors CSPScripts structure exactly:
 *   extends TestBase, dataProvider = commonDataProvider, setTestData(data)
 */
public class DemoWebScripts extends TestBase {

    @Test(description = "Verify Successful Login",
          dataProvider = "commonDataProvider",
          dataProviderClass = DataProvider.class)
    public void TC001_DEMO_Successful_Login(Map<String, String> data) {
        setTestData(data);
        PageObjectInitiatorDemo.getObjectLoginPage().loginToApplication();
    }

//    @Test(description = "Verify Successful Login and Logout",
//          dataProvider = "commonDataProvider",
//          dataProviderClass = DataProvider.class)
//    public void TC002_DEMO_Successful_Login_Logout(Map<String, String> data) {
//        setTestData(data);
//        PageObjectInitiatorDemo.getObjectLoginPage().loginToApplication();
//        PageObjectInitiatorDemo.getObjectLoginPage().logOutApplication();
//    }

}
