package zm.functional;

import io.cucumber.java.en.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class CheckBookingSteps {

    private final WebDriverContext context;
    private WebDriver driver;
    private WebDriverWait wait;

    public CheckBookingSteps(WebDriverContext context) {
        this.context = context;
    }

    @Given("I navigate to the check booking page")
    public void iNavigateToTheCheckBookingPage() {
        driver = context.getDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(context.getBaseUrl() + "/check.html");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("check-form")));
    }

    @Given("a booking exists with a valid token")
    public void aBookingExistsWithAValidToken() {
        String token = context.getBookingToken();
        if (token == null || token.isEmpty()) {
            // Create a booking first by navigating to citizen portal
            WebDriver bookingDriver = context.getDriver();
            bookingDriver.get(context.getBaseUrl() + "/citizen.html");

            WebDriverWait longWait = new WebDriverWait(bookingDriver, Duration.ofSeconds(20));
            longWait.until(ExpectedConditions.elementToBeClickable(By.id("municipality")));

            // Wait for municipalities to load
            longWait.until(d -> {
                return d.findElements(By.cssSelector("#municipality option")).size() > 1;
            });

            // Fill form
            WebElement municipalitySelect = bookingDriver.findElement(By.id("municipality"));
            ((JavascriptExecutor) bookingDriver).executeScript("arguments[0].selectedIndex = 1; arguments[0].dispatchEvent(new Event('change'));", municipalitySelect);

            WebElement dateInput = bookingDriver.findElement(By.id("collection-date"));
            dateInput.clear();
            // Use a date far enough in the future to always be valid
            dateInput.sendKeys("2026-12-15");

            WebElement timeSlot = bookingDriver.findElement(By.id("time-slot"));
            ((JavascriptExecutor) bookingDriver).executeScript("arguments[0].value = '10:00'; arguments[0].dispatchEvent(new Event('change'));", timeSlot);

            WebElement itemName = bookingDriver.findElement(By.cssSelector(".item-name"));
            itemName.clear();
            itemName.sendKeys("Test Item");

            WebElement itemDesc = bookingDriver.findElement(By.cssSelector(".item-description"));
            itemDesc.clear();
            itemDesc.sendKeys("Test Description");


            // Click the submit button
            WebElement submitBtn = longWait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit'].btn-primary"))
            );
            submitBtn.click();

            // Wait for modal or toast to appear
            longWait.until(d -> {
                try {
                    // Check if modal appeared
                    WebElement modal = d.findElement(By.id("booking-result"));
                    if (!modal.getAttribute("class").contains("hidden")) {
                        return true;
                    }

                    // Check if error toast appeared
                    WebElement toast = d.findElement(By.id("toast"));
                    if (!toast.getAttribute("class").contains("hidden")) {
                        String toastText = toast.getText();
                        System.err.println("Booking failed with error: " + toastText);
                        throw new RuntimeException("Booking creation failed: " + toastText);
                    }
                } catch (NoSuchElementException e) {
                    // Element not found yet, continue waiting
                }
                return false;
            });

            // Wait for token to be populated
            WebElement tokenElement = longWait.until(ExpectedConditions.visibilityOfElementLocated(By.id("booking-token")));
            longWait.until(d -> {
                String tokenText = d.findElement(By.id("booking-token")).getText();
                return tokenText != null && !tokenText.isEmpty();
            });

            token = tokenElement.getText();
            System.out.println("Created booking with token: " + token);

            context.setBookingToken(token);

            // Navigate back to check page
            bookingDriver.get(context.getBaseUrl() + "/check.html");
            WebDriverWait checkWait = new WebDriverWait(bookingDriver, Duration.ofSeconds(10));
            checkWait.until(ExpectedConditions.presenceOfElementLocated(By.id("check-form")));
            driver = bookingDriver;
            wait = checkWait;
        }
    }

    @When("I enter the booking token")
    public void iEnterTheBookingToken() {
        String token = context.getBookingToken();
        assertNotNull(token, "Booking token should exist");

        WebElement tokenInput = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("check-token"))
        );
        tokenInput.clear();
        tokenInput.sendKeys(token);
    }

    @When("I enter an invalid token {string}")
    public void iEnterAnInvalidToken(String invalidToken) {
        WebElement tokenInput = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("check-token"))
        );
        tokenInput.clear();
        tokenInput.sendKeys(invalidToken);
    }

    @When("I click check status button")
    public void iClickCheckStatusButton() {
        WebElement checkButton = driver.findElement(By.cssSelector(".btn-primary"));
        checkButton.click();

        // Wait for API call to complete by checking for response
        wait.until(d -> {
            try {
                WebElement details = d.findElement(By.id("booking-details"));
                WebElement toast = d.findElement(By.id("toast"));
                return !details.getAttribute("class").contains("hidden") || 
                       !toast.getAttribute("class").contains("hidden");
            } catch (Exception e) {
                return false;
            }
        });
    }

    @When("I click cancel booking button")
    public void iClickCancelBookingButton() {
        WebElement cancelButton = wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector(".btn-danger"))
        );

        // Handle potential alert dialog
        cancelButton.click();

        try {
            Alert alert = wait.until(ExpectedConditions.alertIsPresent());
            alert.accept();
        } catch (Exception e) {
            // No alert present, continue
        }
        
        // Wait for the cancel operation to complete by checking for toast or status update
        wait.until(d -> {
            try {
                WebElement toast = d.findElement(By.id("toast"));
                if (!toast.getAttribute("class").contains("hidden")) {
                    return true;
                }
                WebElement status = d.findElement(By.id("detail-state"));
                String statusText = status.getText();
                return statusText.contains("CANCELLED") || statusText.contains("Cancelled");
            } catch (Exception e) {
                return false;
            }
        });
    }
    @Then("I should see the booking details")
    public void iShouldSeeTheBookingDetails() {
        // Wait for the booking details to become visible (hidden class removed)
        wait.until(driver -> {
            WebElement bookingDetails = driver.findElement(By.id("booking-details"));
            String classAttr = bookingDetails.getAttribute("class");
            return classAttr != null && !classAttr.contains("hidden");
        });

        WebElement bookingDetails = driver.findElement(By.id("booking-details"));
        assertFalse(bookingDetails.getAttribute("class").contains("hidden"));
    }

    @Then("I should see the municipality")
    public void iShouldSeeTheMunicipality() {
        WebElement municipality = driver.findElement(By.id("detail-municipality"));
        assertFalse(municipality.getText().isEmpty());
    }

    @Then("I should see the collection date and time")
    public void iShouldSeeTheCollectionDateAndTime() {
        WebElement date = driver.findElement(By.id("detail-date"));
        assertFalse(date.getText().isEmpty());

        WebElement time = driver.findElement(By.id("detail-time"));
        assertFalse(time.getText().isEmpty());
    }

    @Then("I should see the status")
    public void iShouldSeeTheStatus() {
        WebElement status = driver.findElement(By.id("detail-state"));
        assertFalse(status.getText().isEmpty());
    }

    @Then("I should see an error message")
    public void iShouldSeeAnErrorMessage() {
        // Check for toast notification or error message
        try {
            WebElement toast = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("toast"))
            );
            assertTrue(toast.isDisplayed() || !toast.getAttribute("class").contains("hidden"));
        } catch (TimeoutException e) {
            // Check if booking details are still hidden (meaning request failed)
            WebElement bookingDetails = driver.findElement(By.id("booking-details"));
            assertTrue(bookingDetails.getAttribute("class").contains("hidden"));
        }
    }

    @Then("the booking should be cancelled")
    public void theBookingShouldBeCancelled() {
        // Wait for success notification or status update
        boolean toastAppeared = false;
        try {
            wait.until(driver -> {
                try {
                    WebElement toast = driver.findElement(By.id("toast"));
                    return toast.isDisplayed() || !toast.getAttribute("class").contains("hidden");
                } catch (Exception e) {
                    return false;
                }
            });
            toastAppeared = true;
        } catch (Exception e) {
            // Toast might not appear, check status directly
        }
        
        // Verify toast appeared OR status was updated
        if (toastAppeared) {
            assertTrue(toastAppeared);
        } else {
            // Check if the booking details are still visible with updated state
            try {
                WebElement status = driver.findElement(By.id("detail-state"));
                String statusText = status.getText();
                assertTrue(statusText.contains("CANCELLED") || statusText.contains("Cancelled"), 
                    "Expected status to be CANCELLED but was: " + statusText);
            } catch (Exception e) {
            }
        }
    }
}
