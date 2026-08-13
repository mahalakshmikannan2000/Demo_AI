package framework.base;

import org.openqa.selenium.WebDriver;


public class ObjectInitiatorFactory {

    private ObjectInitiatorFactory() {
        throw new UnsupportedOperationException("Factory class — do not instantiate.");
    }


    public static void objectInitiator(WebDriver driver, String environment) {
        switch (environment.toUpperCase()) {
            case "DEMO" -> PageObjectInitiatorDemo.objectInitiator(driver);
            default -> throw new IllegalArgumentException("Unsupported environment: " + environment);
        }
    }

    public static void apiObjectInitiator(String environment) {
        switch (environment.toUpperCase()) {
            case "DEMO" -> PageObjectInitiatorDemo.demoAPIObjectInitiator();
            default -> throw new IllegalArgumentException("Unsupported environment: " + environment);
        }
    }
}
