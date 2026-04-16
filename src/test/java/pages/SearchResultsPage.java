package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.ConfigReader;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SearchResultsPage extends BasePage {
    private final By productItems = By.cssSelector("div.product-item");
    private final By productTitleInItem = By.cssSelector("h2.product-title a");
    private final By addToCartButtonInItem = By.cssSelector("input[value='Add to cart']");
    private final By addedToCartBar = By.cssSelector("div.bar-notification.success p.content");
    private final By barNotification = By.id("bar-notification");
    private final By barNotificationClose = By.cssSelector("div.bar-notification span.close");
    private final By cartLink = By.cssSelector("a.ico-cart");
    private final By cartQty = By.cssSelector("span.cart-qty");
    private final By productDetailsAddToCartButton = By.cssSelector("input.button-1.add-to-cart-button");

    // Initializes the search results page with the shared browser instance.
    public SearchResultsPage(WebDriver driver) {
        super(driver);
    }

    // Adds the first available product to cart and returns its visible product name.
    public String addFirstAvailableProductToCart() {
        int qtyBefore = getCartQuantity();
        List<WebElement> items = driver.findElements(productItems);
        for (WebElement item : items) {
            List<WebElement> addButtons = item.findElements(addToCartButtonInItem);
            if (!addButtons.isEmpty() && addButtons.get(0).isEnabled()) {
                String productName = item.findElement(productTitleInItem).getText().trim();
                addButtons.get(0).click();

                // Some products redirect to details page before they can be added.
                if (!driver.findElements(productDetailsAddToCartButton).isEmpty()) {
                    driver.findElements(productDetailsAddToCartButton).get(0).click();
                }

                if (waitForCartQuantityToIncrease(qtyBefore)) {
                    return productName;
                }
            }
        }
        throw new IllegalStateException("No add-to-cart product found in search results");
    }

    // Verifies add-to-cart success by checking either the toast message or cart count.
    public boolean isAddedToCartMessageDisplayed() {
        long timeout = TimeUnit.SECONDS.toMillis(ConfigReader.getInt("explicit.wait.seconds"));
        long end = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < end) {
            List<WebElement> messages = driver.findElements(addedToCartBar);
            if (!messages.isEmpty() && messages.get(0).getText().contains("The product has been added to your shopping cart")) {
                return true;
            }
            // The top bar can disappear quickly, so cart count is a reliable secondary signal.
            if (getCartQuantity() > 0) {
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

    // Opens the cart after clearing any blocking notification banner.
    public CartPage openCart() {
        waitForNotificationToClear();
        click(cartLink);
        return new CartPage(driver);
    }

    // Closes the success toast and waits until it no longer covers the page.
    private void waitForNotificationToClear() {
        List<WebElement> closeButtons = driver.findElements(barNotificationClose);
        if (!closeButtons.isEmpty() && closeButtons.get(0).isDisplayed()) {
            closeButtons.get(0).click();
        }

        // The cart link can be covered by the success bar until the notification fully closes.
        new WebDriverWait(driver, Duration.ofSeconds(ConfigReader.getInt("explicit.wait.seconds")))
                .until(d -> {
                    List<WebElement> bars = d.findElements(barNotification);
                    return bars.isEmpty() || !bars.get(0).isDisplayed();
                });
    }

    // Extracts the numeric cart count from the mini-cart label.
    private int getCartQuantity() {
        // The mini-cart node is frequently re-rendered right after add-to-cart.
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                List<WebElement> qtyElements = driver.findElements(cartQty);
                if (qtyElements.isEmpty()) {
                    return 0;
                }
                String raw = qtyElements.get(0).getText().replaceAll("[^0-9]", "");
                return raw.isEmpty() ? 0 : Integer.parseInt(raw);
            } catch (StaleElementReferenceException ignored) {
                // Retry with a fresh element reference.
            }
        }
        return 0;
    }

    // Waits until the cart quantity reflects that a new item was added.
    private boolean waitForCartQuantityToIncrease(int previousQty) {
        return new WebDriverWait(driver, Duration.ofSeconds(ConfigReader.getInt("explicit.wait.seconds")))
                .until(d -> getCartQuantity() > previousQty);
    }
}
