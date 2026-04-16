package tests;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

public class E2EAutomation {
    private static final String BASE_URL = "https://demowebshop.tricentis.com";
    private static final String EMAIL = "qa.user123@mailinator.com";
    private static final String PASSWORD = "Engineer@09876";
    private static final String SEARCH_KEYWORD = "computer";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    private final By loginLink = By.cssSelector("a.ico-login");
    private final By logoutLink = By.cssSelector("a.ico-logout");
    private final By accountLink = By.cssSelector("a.account");
    private final By emailInput = By.id("Email");
    private final By passwordInput = By.id("Password");
    private final By loginButton = By.cssSelector("input.button-1.login-button");
    private final By searchBox = By.id("small-searchterms");
    private final By searchButton = By.cssSelector("input.button-1.search-box-button");
    private final By productItems = By.cssSelector("div.product-item");
    private final By productTitleInItem = By.cssSelector("h2.product-title a");
    private final By addToCartButtonInItem = By.cssSelector("input[value='Add to cart']");
    private final By productDetailsAddToCartButton = By.cssSelector("input.button-1.add-to-cart-button");
    private final By addedToCartBar = By.cssSelector("div.bar-notification.success p.content");
    private final By barNotification = By.id("bar-notification");
    private final By barNotificationClose = By.cssSelector("div.bar-notification span.close");
    private final By cartLink = By.cssSelector("a.ico-cart");
    private final By cartQty = By.cssSelector("span.cart-qty");
    private final By termsCheckbox = By.id("termsofservice");
    private final By checkoutButton = By.id("checkout");
    private final By billingAddressSelect = By.id("billing-address-select");
    private final By billingCountry = By.id("BillingNewAddress_CountryId");
    private final By billingState = By.id("BillingNewAddress_StateProvinceId");
    private final By billingCity = By.id("BillingNewAddress_City");
    private final By billingAddress1 = By.id("BillingNewAddress_Address1");
    private final By billingZip = By.id("BillingNewAddress_ZipPostalCode");
    private final By billingPhone = By.id("BillingNewAddress_PhoneNumber");
    private final By billingContinue = By.cssSelector("input.button-1.new-address-next-step-button");
    private final By shippingAddressContinue = By.cssSelector("#shipping-buttons-container input.button-1.new-address-next-step-button");
    private final By shippingMethodOptions = By.cssSelector("input[name='shippingoption']");
    private final By shippingMethodContinue = By.cssSelector("input.button-1.shipping-method-next-step-button");
    private final By paymentMethodOptions = By.cssSelector("input[name='paymentmethod']");
    private final By paymentMethodContinue = By.cssSelector("input.button-1.payment-method-next-step-button");
    private final By paymentInfoContinue = By.cssSelector("input.button-1.payment-info-next-step-button");
    private final By confirmOrderButton = By.cssSelector("input.button-1.confirm-order-next-step-button, input[value='Confirm'], input[onclick*='ConfirmOrder.save']");
    private final By orderCompletedContinue = By.cssSelector("input.button-1.order-completed-continue-button");
    private final By orderSuccessMessage = By.cssSelector("div.order-completed div.title strong");

    private WebDriver driver;
    private WebDriverWait wait;

    // Creates a browser session with local defaults for the standalone test.
    @BeforeMethod
    public void setUp() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, TIMEOUT);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
    }

    // Closes the standalone browser session after the test.
    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // Runs the complete purchase flow without using page objects or external helper classes.
    @Test(description = "Standalone E2E purchase flow without page objects or external config")
    public void shouldCompletePurchaseFlowWithoutPageObjects() {
        driver.get(BASE_URL);

        // Login
        click(loginLink);
        type(emailInput, EMAIL);
        type(passwordInput, PASSWORD);
        click(loginButton);

        Assert.assertTrue(isDisplayed(accountLink), "Login failed: account link was not visible.");
        Assert.assertTrue(isDisplayed(logoutLink), "Login failed: logout link was not visible.");

        type(searchBox, SEARCH_KEYWORD);
        click(searchButton);

        // Search results can include both direct add-to-cart items and detail-page items.
        String selectedProduct = addFirstAvailableProductToCart();
        Assert.assertTrue(waitForAddedToCartConfirmation(), "Add-to-cart confirmation was not displayed.");

        closeNotificationIfPresent();
        click(cartLink);

        By productNameInCart = By.xpath("//table[contains(@class,'cart')]//a[contains(normalize-space(),\"" + selectedProduct + "\")]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(productNameInCart));
        Assert.assertTrue(isDisplayed(productNameInCart), "Selected product is not present in the cart.");

        click(termsCheckbox);
        click(checkoutButton);

        completeBillingIfNeeded();
        continueIfVisible(shippingAddressContinue);
        selectFirstVisibleRadioIfNoneSelected(shippingMethodOptions);
        continueIfVisible(shippingMethodContinue);
        selectFirstVisibleRadioIfNoneSelected(paymentMethodOptions);
        continueIfVisible(paymentMethodContinue);
        continueIfVisible(paymentInfoContinue);

        wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(confirmOrderButton),
                ExpectedConditions.visibilityOfElementLocated(orderCompletedContinue)
        ));

        if (isDisplayed(confirmOrderButton)) {
            click(confirmOrderButton);
        }

        wait.until(driver -> {
            String currentUrl = driver.getCurrentUrl().toLowerCase();
            boolean onCompletedUrl = currentUrl.contains("checkout/completed");
            boolean successVisible = hasVisibleText(orderSuccessMessage, "Your order has been successfully processed!");
            boolean continueVisible = isDisplayed(orderCompletedContinue);
            return onCompletedUrl || successVisible || continueVisible;
        });

        Assert.assertTrue(
                hasVisibleText(orderSuccessMessage, "Your order has been successfully processed!")
                        || driver.getCurrentUrl().toLowerCase().contains("checkout/completed"),
                "Order success message was not displayed."
        );

        if (isDisplayed(orderCompletedContinue)) {
            click(orderCompletedContinue);
        } else {
            // Keep logout validation deterministic if the site skips the final continue button.
            driver.navigate().to(BASE_URL + "/");
        }

        click(logoutLink);
        Assert.assertTrue(isDisplayed(loginLink), "Logout failed: login link was not visible.");
    }

    // Adds the first purchasable result and returns the chosen product name.
    private String addFirstAvailableProductToCart() {
        int qtyBefore = getCartQuantity();
        wait.until(ExpectedConditions.visibilityOfElementLocated(productItems));

        List<WebElement> items = driver.findElements(productItems);
        for (WebElement item : items) {
            List<WebElement> addButtons = item.findElements(addToCartButtonInItem);
            if (!addButtons.isEmpty() && addButtons.get(0).isDisplayed() && addButtons.get(0).isEnabled()) {
                String productName = item.findElement(productTitleInItem).getText().trim();
                addButtons.get(0).click();

                if (isDisplayed(productDetailsAddToCartButton)) {
                    click(productDetailsAddToCartButton);
                }

                wait.until(driver -> getCartQuantity() > qtyBefore);
                return productName;
            }
        }

        throw new IllegalStateException("No add-to-cart product found in search results.");
    }

    // Waits until the UI confirms that the product was added to cart.
    private boolean waitForAddedToCartConfirmation() {
        try {
            wait.until(driver -> {
                boolean messageVisible = hasVisibleText(addedToCartBar, "The product has been added to your shopping cart");
                return messageVisible || getCartQuantity() > 0;
            });
            return true;
        } catch (TimeoutException ex) {
            return false;
        }
    }

    // Fills new-address fields only when checkout is asking for a fresh billing address.
    private void completeBillingIfNeeded() {
        if (isDisplayed(billingAddressSelect)) {
            Select select = new Select(driver.findElement(billingAddressSelect));
            String selectedText = select.getFirstSelectedOption().getText().trim().toLowerCase();
            if (selectedText.contains("new")) {
                // Only fill fields when the checkout is asking for a brand-new address.
                selectCountryIfPresent("India");
                selectStateIfPresent("Other");
                typeIfEmpty(billingCity, "Bangalore");
                typeIfEmpty(billingAddress1, "123 Test Street");
                typeIfEmpty(billingZip, "560001");
                typeIfEmpty(billingPhone, "9999999999");
            }
        }
        continueIfVisible(billingContinue);
    }

    // Selects the requested country when the country dropdown is available.
    private void selectCountryIfPresent(String country) {
        if (isDisplayed(billingCountry)) {
            new Select(driver.findElement(billingCountry)).selectByVisibleText(country);
        }
    }

    // Waits for state options to load and selects the requested state.
    private void selectStateIfPresent(String state) {
        if (isDisplayed(billingState)) {
            wait.until(driver -> driver.findElement(billingState).findElements(By.tagName("option")).size() > 1);
            new Select(driver.findElement(billingState)).selectByVisibleText(state);
        }
    }

    // Populates a field only when the element is visible and currently blank.
    private void typeIfEmpty(By locator, String value) {
        if (!isDisplayed(locator)) {
            return;
        }

        WebElement element = driver.findElement(locator);
        if (element.getAttribute("value") == null || element.getAttribute("value").isBlank()) {
            element.clear();
            element.sendKeys(value);
        }
    }

    // Selects the first usable radio button when the page has not chosen one yet.
    private void selectFirstVisibleRadioIfNoneSelected(By locator) {
        List<WebElement> options = driver.findElements(locator);
        if (options.isEmpty()) {
            return;
        }

        for (WebElement option : options) {
            if (option.isDisplayed() && option.isSelected()) {
                return;
            }
        }

        for (WebElement option : options) {
            if (option.isDisplayed() && option.isEnabled()) {
                option.click();
                return;
            }
        }
    }

    // Clicks the locator only when it is currently displayed.
    private void continueIfVisible(By locator) {
        if (isDisplayed(locator)) {
            click(locator);
        }
    }

    // Closes the success banner so it does not block the cart link.
    private void closeNotificationIfPresent() {
        if (isDisplayed(barNotificationClose)) {
            driver.findElement(barNotificationClose).click();
        }

        wait.until(driver -> {
            List<WebElement> notifications = driver.findElements(barNotification);
            return notifications.isEmpty() || !notifications.get(0).isDisplayed();
        });
    }

    // Reads the numeric quantity from the mini-cart label.
    private int getCartQuantity() {
        List<WebElement> quantityElements = driver.findElements(cartQty);
        if (quantityElements.isEmpty()) {
            return 0;
        }

        String raw = quantityElements.get(0).getText().replaceAll("[^0-9]", "");
        return raw.isEmpty() ? 0 : Integer.parseInt(raw);
    }

    // Confirms both visibility and expected text for a locator.
    private boolean hasVisibleText(By locator, String expectedText) {
        List<WebElement> elements = driver.findElements(locator);
        return !elements.isEmpty()
                && elements.get(0).isDisplayed()
                && elements.get(0).getText().contains(expectedText);
    }

    // Checks whether the first element matching the locator is visible.
    private boolean isDisplayed(By locator) {
        List<WebElement> elements = driver.findElements(locator);
        return !elements.isEmpty() && elements.get(0).isDisplayed();
    }

    // Performs a clickable wait before clicking the locator.
    private void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    // Waits for a field to be visible, clears it, and enters the new value.
    private void type(By locator, String value) {
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        element.clear();
        element.sendKeys(value);
    }
}
