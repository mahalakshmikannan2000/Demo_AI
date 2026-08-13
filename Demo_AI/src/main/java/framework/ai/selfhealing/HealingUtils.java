package framework.ai.selfhealing;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class HealingUtils {

    private static final Logger logger = LoggerFactory.getLogger(HealingUtils.class);

    private HealingUtils() {
    }

    /**
     * Attempts to find an element using fallback strategies
     * when the primary locator has failed.
     * Returns healed WebElement or null if all strategies fail.
     */
    public static WebElement tryHeal(WebDriver driver, WebElement failedElement,String tagName) {
        if (tagName == null) {
            WebElement ele = handlenullTagName(driver);
            return ele;
        }
        if (tagName.equalsIgnoreCase("input")) {
            try {
                // STRATEGY 1 — try finding first visible input on page
                List<WebElement> input = driver.findElements(By.xpath("//input[@type='text']"));
                if (!input.isEmpty()) {
                    logger.warn("[SELF-HEAL] Strategy 1 healed — used first text input");
                    return input.get(0);
                }
            } catch (Exception e) {

            }
            try {
                List<WebElement> input = driver.findElements(By.xpath("//input[@placeholder]"));
                if (!input.isEmpty()) {
                    logger.warn("[SELF-HEAL] Strategy 2 healed — used first input with placeholder");
                    return input.get(0);
                }
            } catch (Exception e) {

            }
        }
        if (tagName.equalsIgnoreCase("button")) {
            try {
                // STRATEGY 1 — try finding first visible input on page
                List<WebElement> input = driver.findElements(By.xpath("//input[@type='submit']"));
                if (!input.isEmpty()) {
                    logger.warn("[SELF-HEAL] Strategy 1 healed — used first button");
                    return input.get(0);
                }
            } catch (Exception e) {

            }
            try {
                List<WebElement> input = driver.findElements(By.xpath("//input[@value='Login']"));
                if (!input.isEmpty()) {
                    logger.warn("[SELF-HEAL] Strategy 2 healed — used first button");
                    return input.get(0);
                }
            } catch (Exception e) {

            }
        }
        if (tagName.equalsIgnoreCase("a")) {
            try {
                // STRATEGY 1 — try finding first visible input on page
                List<WebElement> input = driver.findElements(By.xpath("//a"));
                if (!input.isEmpty()) {
                    logger.warn("[SELF-HEAL] Strategy 1 healed — used first link");
                    return input.get(0);
                }
            } catch (Exception e) {

            }
        }

        return null;
    }

    public static WebElement handlenullTagName(WebDriver driver){
        try {
            // STRATEGY 1 — try finding first visible input on page
            List<WebElement> input = driver.findElements(By.xpath("//input[@type='text']"));
            if (!input.isEmpty()) {
                logger.warn("[SELF-HEAL] Strategy 1 healed — used first text input");
                return input.get(0);
            }
        } catch (Exception e) {

        }
        try {
            List<WebElement> input = driver.findElements(By.xpath("//input[@placeholder]"));
            if (!input.isEmpty()) {
                logger.warn("[SELF-HEAL] Strategy 2 healed — used first input with placeholder");
                return input.get(0);
            }
        } catch (Exception e) {

        }
        try {
            List<WebElement> input = driver.findElements(By.xpath("//button"));
            if (!input.isEmpty()) {
                logger.warn("[SELF-HEAL] Strategy 3 healed — used first button");
                return input.get(0);
            }
        } catch (Exception e) {

        }
        try {
            List<WebElement> input = driver.findElements(By.xpath("//a"));
            if (!input.isEmpty()) {
                logger.warn("[SELF-HEAL] Strategy 4 healed — used first Link");
                return input.get(0);
            }
        } catch (Exception e) {

        }

        try {
            List<WebElement> input = driver.findElements(By.xpath("//*[@onclick]"));
            if (!input.isEmpty()) {
                logger.warn("[SELF-HEAL] Strategy 5 healed — used first visible Element");
                return input.get(0);
            }
        } catch (Exception e) {

        }
    return null;
    }
}
