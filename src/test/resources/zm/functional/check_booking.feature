Feature: Check Booking Status
  As a citizen
  I want to check my booking status
  So that I can track my collection request

  Background:
    Given the application is running
    And I navigate to the check booking page

  Scenario: Check booking with valid token
    Given a booking exists with a valid token
    When I enter the booking token
    And I click check status button
    Then I should see the booking details
    And I should see the municipality
    And I should see the collection date and time
    And I should see the status

  Scenario: Check booking with invalid token
    When I enter an invalid token "INVALID123"
    And I click check status button
    Then I should see an error message

  Scenario: Cancel an existing booking
    Given a booking exists with a valid token
    When I enter the booking token
    And I click check status button
    And I click cancel booking button
    Then the booking should be cancelled