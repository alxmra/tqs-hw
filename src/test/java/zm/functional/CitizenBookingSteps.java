package zm.functional;

import io.cucumber.java.en.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class CitizenBookingSteps {

    private final WebDriverContext context;
    private String bookingToken;

    public CitizenBookingSteps(WebDriverContext context) {
        this.context = context;
    }

    private WebDriver getDriver() {
        return context.getDriver();
    }

    private WebDriverWait getWait() {
        return new WebDriverWait(getDriver(), Duration.ofSeconds(10));
    }

    @When("I select a municipality from the list")
    public void iSelectAMunicipalityFromTheList() {
        WebDriverWait wait = getWait();
        WebElement municipalitySelect = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("municipality"))
        );
        
        // Wait for options to load
        wait.until(d -> {
            Select select = new Select(municipalitySelect);
            return select.getOptions().size() > 1;
        });
        
        Select select = new Select(municipalitySelect);
        // Select first available municipality (skip the placeholder)
        if (select.getOptions().size() > 1) {
            select.selectByIndex(1);
        }
    }

    @When("I select a collection date")
    public void iSelectACollectionDate() {
        WebElement dateInput = getDriver().findElement(By.id("collection-date"));
        // Select a future weekday
        LocalDate futureDate = LocalDate.now().plusDays(3);
        while (futureDate.getDayOfWeek().getValue() > 5) {
            futureDate = futureDate.plusDays(1);
        }
        dateInput.sendKeys(futureDate.toString());
    }

    @When("I select a time slot")
    public void iSelectATimeSlot() {
        Select timeSlotSelect = new Select(getDriver().findElement(By.id("time-slot")));
        timeSlotSelect.selectByValue("10:00");
    }

    @When("I add an item with name {string} and description {string}")
    public void iAddAnItemWithNameAndDescription(String name, String description) {
        WebElement itemName = getDriver().findElement(By.cssSelector(".item-entry:last-child .item-name"));
        WebElement itemDesc = getDriver().findElement(By.cssSelector(".item-entry:last-child .item-description"));
        
        itemName.clear();
        itemName.sendKeys(name);
        itemDesc.clear();
        itemDesc.sendKeys(description);
    }

    @When("I click add item button")
    public void iClickAddItemButton() {
        WebElement addButton = getDriver().findElement(By.id("add-item-btn"));
        addButton.click();
        // Wait for new item entry to appear
        WebDriverWait wait = getWait();
        wait.until(d -> d.findElements(By.cssSelector(".item-entry")).size() > 1);
    }

    @When("I add another item with name {string} and description {string}")
    public void iAddAnotherItemWithNameAndDescription(String name, String description) {
        WebElement lastItemName = getDriver().findElement(
            By.cssSelector(".item-entry:last-child .item-name")
        );
        WebElement lastItemDesc = getDriver().findElement(
            By.cssSelector(".item-entry:last-child .item-description")
        );
        
        lastItemName.sendKeys(name);
        lastItemDesc.sendKeys(description);
    }

    @When("I submit the booking form")
    public void iSubmitTheBookingForm() {
        WebElement submitButton = getDriver().findElement(
            By.cssSelector("#booking-form button[type='submit']")
        );
        submitButton.click();
    }

    @When("I submit the booking form without filling required fields")
    public void iSubmitTheBookingFormWithoutFillingRequiredFields() {
        // Don't fill any fields, just try to submit
        WebElement submitButton = getDriver().findElement(
            By.cssSelector("#booking-form button[type='submit']")
        );
        
        // Try to submit without filling required fields
        // HTML5 validation should prevent submission
        submitButton.click();
        
        // Wait briefly to ensure validation has a chance to trigger
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Then("I should see a success message")
    public void iShouldSeeASuccessMessage() {
        WebDriverWait wait = getWait();
        WebElement resultModal = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("booking-result"))
        );
        assertTrue(resultModal.getAttribute("class").contains("hidden") == false 
                || resultModal.isDisplayed());
        
        WebElement successText = getDriver().findElement(
            By.xpath("//*[contains(text(), 'Booking Confirmed')]")
        );
        assertTrue(successText.isDisplayed());
    }

    @Then("I should receive a booking token")
    public void iShouldReceiveABookingToken() {
        WebDriverWait wait = getWait();
        WebElement tokenElement = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("booking-token"))
        );
        bookingToken = tokenElement.getText();
        assertNotNull(bookingToken);
        assertFalse(bookingToken.isEmpty());
        context.setBookingToken(bookingToken);
    }

    @Then("the form should not be submitted")
    public void theFormShouldNotBeSubmitted() {
        // Check that we're still on the form (result modal is not visible)
        WebElement form = getDriver().findElement(By.id("booking-form"));
        assertTrue(form.isDisplayed());
        
        // HTML5 validation should prevent submission
        WebElement municipalityField = getDriver().findElement(By.id("municipality"));
        String validationMessage = municipalityField.getAttribute("validationMessage");
        assertNotNull(validationMessage);
    }
}