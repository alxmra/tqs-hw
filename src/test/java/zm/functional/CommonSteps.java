package zm.functional;

import io.cucumber.java.en.*;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.*;

public class CommonSteps {

    private final WebDriverContext context;

    public CommonSteps(WebDriverContext context) {
        this.context = context;
    }

    @Given("the application is running")
    public void theApplicationIsRunning() {
        WebDriver driver = context.getDriver();
        assertNotNull(driver, "WebDriver should be initialized");
        // Application accessibility will be verified when navigating to specific pages
    }
}