package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class CartPage extends BasePage {
    private final By termsCheckbox = By.id("termsofservice");
    private final By checkoutButton = By.id("checkout");

    // Initializes the cart page with the shared browser instance.
    public CartPage(WebDriver driver) {
        super(driver);
    }

    // Verifies that the selected product name appears in the cart table.
    public boolean isProductInCart(String productName) {
        By productNameInCart = By.xpath("//table[contains(@class,'cart')]//a[contains(normalize-space(),\"" + productName + "\")]");
        return !driver.findElements(productNameInCart).isEmpty();
    }

    // Accepts the terms and moves the user from cart to checkout.
    public CheckoutPage proceedToCheckout() {
        click(termsCheckbox);
        click(checkoutButton);
        return new CheckoutPage(driver);
    }
}
