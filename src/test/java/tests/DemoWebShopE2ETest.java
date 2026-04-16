package tests;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.CartPage;
import pages.CheckoutPage;
import pages.HomePage;
import pages.LoginPage;
import pages.OrderConfirmationPage;
import pages.SearchResultsPage;
import utils.ConfigReader;
import utils.DriverFactory;

public class DemoWebShopE2ETest {
    private static final Logger log = LoggerFactory.getLogger(DemoWebShopE2ETest.class);

    private WebDriver driver;

    // Creates a fresh driver and opens the application before each TestNG test.
    @BeforeMethod
    public void setUp() {
        driver = DriverFactory.createDriver();
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(ConfigReader.getInt("implicit.wait.seconds")));
        driver.get(ConfigReader.get("base.url"));
    }

    // Closes the browser after each TestNG test.
    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    // Covers login, product selection, checkout, order confirmation, and logout in one flow.
    @Test(description = "Complete purchase flow on Demo Webshop")
    public void shouldCompletePurchaseFlow() {
        HomePage homePage = new HomePage(driver);
        LoginPage loginPage = homePage.clickLogin();

        homePage = loginPage.loginAs(ConfigReader.get("email"), ConfigReader.get("password"));
        Assert.assertTrue(homePage.isUserLoggedIn(), "Login failed - expected account links are not visible.");
        log.info("Login successful");

        SearchResultsPage searchResultsPage = homePage.searchFor(ConfigReader.get("search.keyword"));
        String selectedProductName = searchResultsPage.addFirstAvailableProductToCart();
        Assert.assertTrue(searchResultsPage.isAddedToCartMessageDisplayed(), "Add to cart confirmation was not shown.");
        log.info("Product added to cart: {}", selectedProductName);

        CartPage cartPage = searchResultsPage.openCart();
        Assert.assertTrue(cartPage.isProductInCart(selectedProductName), "Selected product is not present in cart.");

        CheckoutPage checkoutPage = cartPage.proceedToCheckout();
        checkoutPage.completeBillingAddress();
        checkoutPage.selectShippingMethodAndContinue();
        checkoutPage.selectPaymentMethodAndContinue();
        checkoutPage.continuePaymentInformation();

        OrderConfirmationPage orderConfirmationPage = checkoutPage.confirmOrder();
        Assert.assertTrue(orderConfirmationPage.isOrderSuccessMessageDisplayed(), "Order success message is not displayed.");
        log.info("Order placed successfully");

        homePage = orderConfirmationPage.completeOrderAndGoHome();
        homePage.logout();
        Assert.assertTrue(homePage.isUserLoggedOut(), "User logout validation failed.");
    }
}
