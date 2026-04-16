package stepdefinitions;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import pages.CartPage;
import pages.CheckoutPage;
import pages.HomePage;
import pages.LoginPage;
import pages.OrderConfirmationPage;
import pages.SearchResultsPage;
import utils.ConfigReader;
import utils.DriverFactory;

public class PurchaseFlowSteps {
    private static final Logger log = LoggerFactory.getLogger(PurchaseFlowSteps.class);
    private WebDriver driver;
    private HomePage homePage;
    private SearchResultsPage searchResultsPage;
    private CartPage cartPage;
    private CheckoutPage checkoutPage;
    private OrderConfirmationPage orderConfirmationPage;
    private String selectedProduct;

    // Creates a fresh browser session before each Cucumber scenario.
    @Before
    public void setUpScenario() {
        driver = DriverFactory.createDriver();
        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(ConfigReader.getInt("implicit.wait.seconds")));
        log.info("Browser initialized");
    }

    // Closes the browser after each scenario finishes.
    @After
    public void tearDownScenario() {
        if (driver != null) {
            driver.quit();
            log.info("Browser closed");
        }
    }

    // Opens the webshop home page and prepares the home page object.
    @Given("user launches the browser and opens demo webshop")
    public void userLaunchesTheBrowserAndOpensDemoWebshop() {
        driver.get(ConfigReader.get("base.url"));
        homePage = new HomePage(driver);
        log.info("Navigated to: {}", ConfigReader.get("base.url"));
    }

    // Logs in with configured credentials and validates the logged-in state.
    @When("user logs in with valid credentials")
    public void userLogsInWithValidCredentials() {
        LoginPage loginPage = homePage.clickLogin();
        homePage = loginPage.loginAs(ConfigReader.get("email"), ConfigReader.get("password"));
        Assert.assertTrue(homePage.isUserLoggedIn(), "Login success validation failed");
        log.info("Logged in as: {}", ConfigReader.get("email"));
    }

    // Searches for the configured product keyword and adds the first valid result to cart.
    @And("user searches for a product and adds first result to cart")
    public void userSearchesForAProductAndAddsFirstResultToCart() {
        searchResultsPage = homePage.searchFor(ConfigReader.get("search.keyword"));
        selectedProduct = searchResultsPage.addFirstAvailableProductToCart();
        Assert.assertTrue(searchResultsPage.isAddedToCartMessageDisplayed(), "Product add-to-cart validation failed");
        log.info("Product searched and added to cart: {}", selectedProduct);
    }

    // Opens the cart and checks that the product selected earlier is present.
    @Then("cart should contain the selected product")
    public void cartShouldContainTheSelectedProduct() {
        cartPage = searchResultsPage.openCart();
        Assert.assertTrue(cartPage.isProductInCart(selectedProduct), "Cart product validation failed");
        log.info("Verified product in cart: {}", selectedProduct);
    }

    // Completes the remaining checkout steps and stores the final confirmation page.
    @When("user proceeds to checkout and completes billing shipping and payment steps")
    public void userProceedsToCheckoutAndCompletesBillingShippingAndPaymentSteps() {
        checkoutPage = cartPage.proceedToCheckout();
        checkoutPage.completeBillingAddress();
        checkoutPage.selectShippingMethodAndContinue();
        checkoutPage.selectPaymentMethodAndContinue();
        checkoutPage.continuePaymentInformation();
        orderConfirmationPage = checkoutPage.confirmOrder();
        log.info("Checkout process completed");
    }

    // Verifies that the order was placed successfully and returns to the home page.
    @Then("order should be placed successfully")
    public void orderShouldBePlacedSuccessfully() {
        Assert.assertTrue(orderConfirmationPage.isOrderSuccessMessageDisplayed(), "Order confirmation validation failed");
        homePage = orderConfirmationPage.completeOrderAndGoHome();
        log.info("Order placed successfully");
    }

    // Logs out from the application and verifies that the user session ended.
    @And("user logs out successfully")
    public void userLogsOutSuccessfully() {
        homePage.logout();
        Assert.assertTrue(homePage.isUserLoggedOut(), "Logout validation failed");
        log.info("Logged out successfully");
    }
}
