package utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GeminiSelfHealer {
    private static final Logger log = LoggerFactory.getLogger(GeminiSelfHealer.class);
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
    private static final Gson GSON = new Gson();

    // Attempts to recover a broken Selenium locator by asking Gemini for a replacement XPath.
    public static By healLocator(WebDriver driver, By oldLocator) {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("GEMINI_API_KEY is not set. Self-healing is disabled.");
            return null;
        }

        log.info("Attempting to heal locator: {}", oldLocator);
        try {
            String pageSource = getSimplifiedDOM(driver);
            String prompt = buildPrompt(oldLocator.toString(), pageSource);
            String response = callGeminiApi(prompt, apiKey);

            if (response != null && !response.trim().isEmpty()) {
                // Normalize common wrapper text so the caller always gets a clean XPath.
                String newXPath = response.trim().replaceAll("^\"|\"$", "").replaceAll("^`+|`+$", "");
                if (newXPath.startsWith("xpath=") || newXPath.startsWith("xpath:") || newXPath.startsWith("XPath:") || newXPath.startsWith("XPath=")) {
                    newXPath = newXPath.substring(6).trim();
                }
                log.info("AI suggested new XPath: {}", newXPath);
                return By.xpath(newXPath);
            }
        } catch (Exception e) {
            log.error("Failed to heal locator: {}", e.getMessage(), e);
        }

        return null;
    }

    // Reads the Gemini API key from the standard config source when available.
    private static String getApiKey() {
        try {
            return ConfigReader.get("gemini.api.key");
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Sends a trimmed DOM snapshot instead of the full page to keep the AI prompt manageable.
    private static String getSimplifiedDOM(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        // Basic script to remove scripts and styles for a cleaner DOM payload
        String script = "var clone = document.documentElement.cloneNode(true);" +
                "var scripts = clone.getElementsByTagName('script');" +
                "while (scripts[0]) scripts[0].parentNode.removeChild(scripts[0]);" +
                "var styles = clone.getElementsByTagName('style');" +
                "while (styles[0]) styles[0].parentNode.removeChild(styles[0]);" +
                "return clone.innerHTML;";
        String rawHtml = (String) js.executeScript(script);
        // Prevent huge payloads if DOM is massive
        return rawHtml.length() > 50000 ? rawHtml.substring(0, 50000) + "...(truncated)" : rawHtml;
    }

    // Builds the instruction sent to Gemini to request a replacement XPath.
    private static String buildPrompt(String oldLocator, String dom) {
        return "You are an AI Self-Healing Selenium Test Case assistant.\n" +
                "A Selenium test failed to find an element using the old locator: " + oldLocator + "\n" +
                "Analyze the provided HTML DOM snippet and provide an updated XPath to reliably locate the intended element.\n" +
                "Return ONLY the exact XPath string. Do not include markdown formatting like ```xpath, do not include quotes or explanations.\n" +
                "HTML DOM:\n" + dom;
    }

    // Calls the Gemini generateContent API and returns the first text response.
    private static String callGeminiApi(String prompt, String apiKey) throws Exception {
        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        
        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonObject jsonResponse = GSON.fromJson(response.body(), JsonObject.class);
            // Gemini returns nested candidates/content/parts; pull the first text answer only.
            return jsonResponse.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        } else {
            throw new RuntimeException("Gemini API call failed with status: " + response.statusCode() + " Body: " + response.body());
        }
    }
}
