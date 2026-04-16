package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.ConfigReader;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class OrderConfirmationPage extends BasePage {
    private final By successMessage = By.cssSelector("div.order-completed div.title strong");
    private final By completionContainer = By.cssSelector("div.order-completed");
    private final By continueButton = By.cssSelector("input.button-1.order-completed-continue-button");

    // Initializes the order confirmation page with the shared browser instance.
    public OrderConfirmationPage(WebDriver driver) {
        super(driver);
    }

    // Confirms order completion using multiple page signals for stability.
    public boolean isOrderSuccessMessageDisplayed() {
        long timeout = TimeUnit.SECONDS.toMillis(ConfigReader.getInt("explicit.wait.seconds"));
        long end = System.currentTimeMillis() + timeout;

        while (System.currentTimeMillis() < end) {
            List<org.openqa.selenium.WebElement> titles = driver.findElements(successMessage);
            if (!titles.isEmpty() && titles.get(0).getText().contains("Your order has been successfully processed!")) {
                return true;
            }

            List<org.openqa.selenium.WebElement> containers = driver.findElements(completionContainer);
            if (!containers.isEmpty() && containers.get(0).getText().contains("Your order has been successfully processed!")) {
                return true;
            }

            if (driver.getCurrentUrl().toLowerCase().contains("checkout/completed")) {
                return true;
            }

            // Final fallback for slightly different confirmation markup.
            if (driver.getPageSource().contains("Your order has been successfully processed!")) {
                return true;
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    // Returns to the home page through the continue button or a direct navigation fallback.
    public HomePage completeOrderAndGoHome() {
        if (!driver.findElements(continueButton).isEmpty() && driver.findElements(continueButton).get(0).isDisplayed()) {
            click(continueButton);
        } else {
            // Recover to the home page even if the final continue button is missing.
            driver.navigate().to("https://demowebshop.tricentis.com/");
        }
        return new HomePage(driver);
    }
}
