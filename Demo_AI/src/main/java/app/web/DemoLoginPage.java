package app.web;

import framework.utils.WebActions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.testng.Assert;

/**
 * Demo login page — targets saucedemo.com
 * Structure mirrors CSPLoginPage:
 *   extends WebActions, PageFactory init, action methods return void
 */
public class DemoLoginPage extends WebActions {

    private static final String LOGIN_SUCCESS  = "Login successful — Products page loaded";
    private static final String LOGOUT_SUCCESS = "Logout successful — Login page loaded";

    public WebDriver driver;

   @FindBy(id = "user-name_broken")
    private WebElement inputUsername;

    @FindBy(id = "password")
    private WebElement inputPassword;

    @FindBy(id = "login-button")
    private WebElement btnLogin;

    @FindBy(id = "react-burger-menu-btn")
    private WebElement btnMenu;

    @FindBy(id = "logout_sidebar_link")
    private WebElement btnLogout;

    @FindBy(css = ".title")
    private WebElement pageTitle;

    public DemoLoginPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    /*
     * Method Description : Logs into saucedemo with credentials from .env
     * Input  : none
     * Output : void
     */
    public void loginToApplication() {
        enterTextBySendKeys(inputUsername, "standard_user");
        enterTextBySendKeys(inputPassword, "secret_sauce");
        clickElement(btnLogin);
        Assert.assertTrue(isElementPresent(pageTitle,WAIT_5_SEC), LOGIN_SUCCESS);
    }

    /*
     * Method Description : Logs out via burger menu
     * Input  : none
     * Output : void
     */
    public void logOutApplication() {
        clickElement(btnMenu);
        clickElement(btnLogout);
        Assert.assertTrue(isElementPresent(inputUsername,WAIT_5_SEC), LOGOUT_SUCCESS);
    }
}
