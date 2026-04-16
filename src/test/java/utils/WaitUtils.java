package utils;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class WaitUtils {
    private final WebDriverWait wait;
    private final WebDriver driver;

    // Creates an explicit wait helper using the configured timeout.
    public WaitUtils(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(ConfigReader.getInt("explicit.wait.seconds")));
    }

    // Waits for visibility and optionally retries with a healed locator on timeout.
    public WebElement visible(By locator) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            // If the original locator breaks, try a healed locator before failing the step.
            By healedLocator = GeminiSelfHealer.healLocator(driver, locator);
            if (healedLocator != null) {
                return wait.until(ExpectedConditions.visibilityOfElementLocated(healedLocator));
            }
            throw e;
        }
    }

    // Waits for clickability and optionally retries with a healed locator on timeout.
    public WebElement clickable(By locator) {
        try {
            return wait.until(ExpectedConditions.elementToBeClickable(locator));
        } catch (TimeoutException e) {
            // Reuse the same healing fallback for clickable elements as well.
            By healedLocator = GeminiSelfHealer.healLocator(driver, locator);
            if (healedLocator != null) {
                return wait.until(ExpectedConditions.elementToBeClickable(healedLocator));
            }
            throw e;
        }
    }

    // Waits for expected text and optionally retries with a healed locator on timeout.
    public boolean textPresent(By locator, String expectedText) {
        try {
            return wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, expectedText));
        } catch (TimeoutException e) {
            // Text assertions can also recover if the DOM changed but the intent is the same.
            By healedLocator = GeminiSelfHealer.healLocator(driver, locator);
            if (healedLocator != null) {
                return wait.until(ExpectedConditions.textToBePresentInElementLocated(healedLocator, expectedText));
            }
            throw e;
        }
    }
}
