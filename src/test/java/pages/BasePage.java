package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import utils.WaitUtils;

public abstract class BasePage {
    protected final WebDriver driver;
    protected final WaitUtils wait;

    // Stores the shared driver and wait helper for all page objects.
    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver);
    }

    // Waits until the target element is visible before returning it.
    protected WebElement visible(By locator) {
        return wait.visible(locator);
    }

    // Waits until the target element is clickable before returning it.
    protected WebElement clickable(By locator) {
        return wait.clickable(locator);
    }

    // Performs a click through the common wait wrapper.
    protected void click(By locator) {
        clickable(locator).click();
    }

    // Clears the field and types the supplied value.
    protected void type(By locator, String value) {
        WebElement element = visible(locator);
        element.clear();
        element.sendKeys(value);
    }

    // Reads trimmed text from a visible element.
    protected String text(By locator) {
        return visible(locator).getText().trim();
    }
}
