package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;
import utils.ConfigReader;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CheckoutPage extends BasePage {
    private final By billingAddressSelect = By.id("billing-address-select");
    private final By billingFirstName = By.id("BillingNewAddress_FirstName");
    private final By billingLastName = By.id("BillingNewAddress_LastName");
    private final By billingEmail = By.id("BillingNewAddress_Email");
    private final By billingCompany = By.id("BillingNewAddress_Company");
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
    private static final String ORDER_SUCCESS_TEXT = "Your order has been successfully processed!";

    // Initializes the checkout page with the shared browser instance.
    public CheckoutPage(WebDriver driver) {
        super(driver);
    }

    // Reuses an existing billing address when possible and advances checkout.
    public void completeBillingAddress() {
        if (!driver.findElements(billingAddressSelect).isEmpty()) {
            Select select = new Select(visible(billingAddressSelect));
            if (!select.getOptions().isEmpty()) {
                String selected = select.getFirstSelectedOption().getText().trim();
                // Reuse an existing saved address when the site offers one.
                if (selected.toLowerCase().contains("new") && select.getOptions().size() > 1) {
                    select.selectByIndex(1);
                }
            }
        }

        clickFirstDisplayed(billingContinue);
        continueShippingAddressIfPresent();
    }

    // Selects a shipping option if needed and advances to the next step.
    public void selectShippingMethodAndContinue() {
        selectFirstRadioIfNoneSelected(shippingMethodOptions);
        clickFirstDisplayed(shippingMethodContinue);
    }

    // Selects a payment method if needed and advances to the next step.
    public void selectPaymentMethodAndContinue() {
        selectFirstRadioIfNoneSelected(paymentMethodOptions);
        clickFirstDisplayed(paymentMethodContinue);
    }

    // Skips payment info when already past it, otherwise advances to order confirmation.
    public void continuePaymentInformation() {
        if (isAnyDisplayed(confirmOrderButton) || isAnyDisplayed(orderCompletedContinue)) {
            return;
        }

        if (isAnyDisplayed(paymentInfoContinue)) {
            clickFirstDisplayed(paymentInfoContinue);
            waitForAnyDisplayed(confirmOrderButton, orderCompletedContinue);
        }
    }

    // Reaches the final confirmation state and returns the order confirmation page object.
    public OrderConfirmationPage confirmOrder() {
        // Some runs land one step earlier than expected, so advance until confirm is really available.
        advanceOnePageCheckoutToConfirm();

        if (isAnyDisplayed(orderCompletedContinue)) {
            return new OrderConfirmationPage(driver);
        }

        waitForAnyDisplayed(confirmOrderButton, orderCompletedContinue);

        if (isAnyDisplayed(orderCompletedContinue)) {
            return new OrderConfirmationPage(driver);
        }

        if (isAnyDisplayed(confirmOrderButton) && clickFirstDisplayed(confirmOrderButton)) {
            waitForOrderCompletion();
        }
        return new OrderConfirmationPage(driver);
    }

    // Clicks through any remaining checkout step until the confirm screen is reached.
    private void advanceOnePageCheckoutToConfirm() {
        long timeout = TimeUnit.SECONDS.toMillis(ConfigReader.getInt("explicit.wait.seconds"));
        long end = System.currentTimeMillis() + timeout;

        while (System.currentTimeMillis() < end) {
            if (isAnyDisplayed(confirmOrderButton) || isAnyDisplayed(orderCompletedContinue)) {
                return;
            }

            boolean clicked = false;
            if (clickFirstDisplayed(billingContinue)) {
                clicked = true;
            } else if (clickFirstDisplayed(shippingAddressContinue)) {
                clicked = true;
            } else if (clickFirstDisplayed(shippingMethodContinue)) {
                clicked = true;
            } else if (clickFirstDisplayed(paymentMethodContinue)) {
                clicked = true;
            } else if (clickFirstDisplayed(paymentInfoContinue)) {
                clicked = true;
            }

            if (!clicked) {
                // Stop once there is no visible checkout action to advance.
                return;
            }

            try {
                Thread.sleep(400);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    // Waits until the site shows that the order has been completed.
    private void waitForOrderCompletion() {
        long timeout = TimeUnit.SECONDS.toMillis(ConfigReader.getInt("explicit.wait.seconds"));
        long end = System.currentTimeMillis() + timeout;

        while (System.currentTimeMillis() < end) {
            if (isAnyDisplayed(orderCompletedContinue)) {
                return;
            }

            String url = driver.getCurrentUrl().toLowerCase();
            if (url.contains("checkout/completed")) {
                return;
            }

            // Fall back to page content because the completion page is not always identical.
            if (driver.getPageSource().contains(ORDER_SUCCESS_TEXT)) {
                return;
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    // Waits until at least one of the supplied locators is displayed.
    private void waitForAnyDisplayed(By... locators) {
        long timeout = TimeUnit.SECONDS.toMillis(ConfigReader.getInt("explicit.wait.seconds"));
        long end = System.currentTimeMillis() + timeout;

        while (System.currentTimeMillis() < end) {
            for (By locator : locators) {
                if (isAnyDisplayed(locator)) {
                    return;
                }
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    // Continues the shipping address step only when that optional section is present.
    private void continueShippingAddressIfPresent() {
        if (!driver.findElements(shippingAddressContinue).isEmpty() && driver.findElements(shippingAddressContinue).get(0).isDisplayed()) {
            click(shippingAddressContinue);
        }
    }

    // Clicks the first visible and enabled element matching the locator.
    private boolean clickFirstDisplayed(By locator) {
        List<WebElement> elements = driver.findElements(locator);
        for (WebElement element : elements) {
            if (element.isDisplayed() && element.isEnabled()) {
                element.click();
                return true;
            }
        }
        return false;
    }

    // Checks whether any matching element is currently displayed on the page.
    private boolean isAnyDisplayed(By locator) {
        List<WebElement> elements = driver.findElements(locator);
        for (WebElement element : elements) {
            if (element.isDisplayed()) {
                return true;
            }
        }
        return false;
    }

    // Selects the first visible radio option only when nothing is already selected.
    private void selectFirstRadioIfNoneSelected(By radioLocator) {
        List<WebElement> options = driver.findElements(radioLocator);
        if (options.isEmpty()) {
            return;
        }

        // Keep the site's default choice if one is already selected.
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
}
