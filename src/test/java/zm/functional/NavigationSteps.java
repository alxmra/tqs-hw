package zm.functional;

import io.cucumber.java.en.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class NavigationSteps {

    private final WebDriverContext context;
    private WebDriver driver;
    private WebDriverWait wait;

    public NavigationSteps(WebDriverContext context) {
        this.context = context;
    }

    @When("I navigate to the home page")
    public void iNavigateToTheHomePage() {
        driver = context.getDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(context.getBaseUrl() + "/index.html");
    }

    @Given("I navigate to the citizen portal")
    public void iNavigateToTheCitizenPortal() {
        driver = context.getDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(context.getBaseUrl() + "/citizen.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("booking-form")));
    }

    @When("I click on the staff dashboard link")
    public void iClickOnTheStaffDashboardLink() {
        WebElement staffLink = wait.until(
            ExpectedConditions.elementToBeClickable(By.xpath("//a[@href='staff.html']"))
        );
        staffLink.click();
    }

    @When("I click on the check booking link")
    public void iClickOnTheCheckBookingLink() {
        WebElement checkLink = wait.until(
            ExpectedConditions.elementToBeClickable(By.xpath("//a[@href='check.html']"))
        );
        checkLink.click();
    }

    @Then("I should be redirected to the citizen portal")
    public void iShouldBeRedirectedToTheCitizenPortal() {
        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("citizen.html"),
            ExpectedConditions.presenceOfElementLocated(By.id("booking-form"))
        ));
        
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("citizen.html"), 
                  "Should be redirected to citizen portal");
    }

    @Then("I should be on the staff dashboard page")
    public void iShouldBeOnTheStaffDashboardPage() {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("bookings-list")));
        
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("staff.html"), 
                  "Should be on staff dashboard page");
        
        WebElement table = driver.findElement(By.id("bookings-list"));
        assertTrue(table.isDisplayed(), "Bookings table should be visible");
    }

    @Then("I should be on the check booking page")
    public void iShouldBeOnTheCheckBookingPage() {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("check-form")));
        
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("check.html"), 
                  "Should be on check booking page");
        
        WebElement checkForm = driver.findElement(By.id("check-form"));
        assertTrue(checkForm.isDisplayed(), "Check form should be visible");
    }
}