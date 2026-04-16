package utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public final class DriverFactory {
    // Prevents utility class instantiation.
    private DriverFactory() {
    }

    // Creates the configured browser instance for the test run.
    public static WebDriver createDriver() {
        String browser = ConfigReader.get("browser");
        if (!"chrome".equalsIgnoreCase(browser)) {
            throw new IllegalArgumentException("Unsupported browser: " + browser + ". Only chrome is configured.");
        }

        // WebDriverManager resolves the matching driver binary automatically.
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        if (ConfigReader.getBoolean("headless")) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");

        return new ChromeDriver(options);
    }
}
