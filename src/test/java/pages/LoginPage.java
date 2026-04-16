package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {
    private final By emailInput = By.id("Email");
    private final By passwordInput = By.id("Password");
    private final By loginButton = By.cssSelector("input.button-1.login-button");

    // Initializes the login page with the shared browser instance.
    public LoginPage(WebDriver driver) {
        super(driver);
    }

    // Submits the login form and returns the expected landing page object.
    public HomePage loginAs(String email, String password) {
        type(emailInput, email);
        type(passwordInput, password);
        click(loginButton);
        return new HomePage(driver);
    }
}
