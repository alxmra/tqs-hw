Feature: Citizen Booking Portal
  As a citizen
  I want to book waste collection services
  So that I can schedule pickup of bulky items

  Background:
    Given the application is running
    And I navigate to the citizen portal

  Scenario: Successfully create a booking
    When I select a municipality from the list
    And I select a collection date
    And I select a time slot
    And I add an item with name "Old Mattress" and description "Queen size"
    And I submit the booking form
    Then I should see a success message
    And I should receive a booking token

  Scenario: Add multiple items to booking
    When I select a municipality from the list
    And I select a collection date
    And I select a time slot
    And I add an item with name "Refrigerator" and description "Old white fridge"
    And I click add item button
    And I add another item with name "Washing Machine" and description "Broken appliance"
    And I submit the booking form
    Then I should see a success message
    And I should receive a booking token

  Scenario: Form validation for required fields
    When I submit the booking form without filling required fields
    Then the form should not be submitted