package framework.base;

import framework.api.APIClient;
import org.openqa.selenium.WebDriver;
import app.web.DemoLoginPage;

/**
 * Mirrors PageObjectInitiatorCSP exactly —
 * ThreadLocal per page/client, objectInitiator(driver) called from
 * ObjectInitiatorFactory, getObjectXxx() returns current thread's instance.
 */
public class PageObjectInitiatorDemo {

    static final ThreadLocal<DemoLoginPage> DEMO_LOGIN_PAGE = new ThreadLocal<>();
    static final ThreadLocal<APIClient>     DEMO_REST_API   = new ThreadLocal<>();

    private PageObjectInitiatorDemo() {
        throw new UnsupportedOperationException("Object Initiator class — do not instantiate.");
    }

    /*
     * Method Description : Initialises Demo web page objects for current thread's WebDriver
     * Input  : WebDriver driver
     * Output : void
     */
    public static void objectInitiator(WebDriver driver) {
        DEMO_LOGIN_PAGE.set(new DemoLoginPage(driver));
    }

    /*
     * Method Description : Initialises Demo REST API client for current thread
     * Input  : none
     * Output : void
     */
    public static void demoAPIObjectInitiator() {
        DEMO_REST_API.set(new APIClient());
    }

    public static DemoLoginPage getObjectLoginPage() {
        return DEMO_LOGIN_PAGE.get();
    }

    public static APIClient getApiClient() {
        return DEMO_REST_API.get();
    }
}
