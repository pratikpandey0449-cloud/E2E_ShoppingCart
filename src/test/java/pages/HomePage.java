package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class HomePage extends BasePage {
    private final By loginLink = By.cssSelector("a.ico-login");
    private final By logoutLink = By.cssSelector("a.ico-logout");
    private final By accountLink = By.cssSelector("a.account");
    private final By searchBox = By.id("small-searchterms");
    private final By searchButton = By.cssSelector("input.button-1.search-box-button");

    // Initializes the home page with the shared browser instance.
    public HomePage(WebDriver driver) {
        super(driver);
    }

    // Opens the login page from the site header.
    public LoginPage clickLogin() {
        click(loginLink);
        return new LoginPage(driver);
    }

    // Searches for the given keyword from the global search box.
    public SearchResultsPage searchFor(String keyword) {
        type(searchBox, keyword);
        click(searchButton);
        return new SearchResultsPage(driver);
    }

    // Confirms a logged-in state by checking account and logout links.
    public boolean isUserLoggedIn() {
        return !driver.findElements(accountLink).isEmpty() && !driver.findElements(logoutLink).isEmpty();
    }

    // Signs the current user out from the header link.
    public void logout() {
        click(logoutLink);
    }

    // Confirms logout by checking that the login link is visible again.
    public boolean isUserLoggedOut() {
        return !driver.findElements(loginLink).isEmpty();
    }
}
