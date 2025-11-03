Feature: Staff Dashboard
  As a staff member
  I want to manage collection bookings
  So that I can update their status and monitor operations

  Background:
    Given the application is running
    And I navigate to the staff dashboard

  Scenario: View all bookings
    When I load the staff dashboard
    Then I should see the bookings table
    And I should see statistics cards

  Scenario: Filter bookings by municipality
    Given multiple bookings exist in different municipalities
    When I select a municipality from the filter
    And I click refresh button
    Then I should only see bookings for that municipality

  Scenario: Update booking state
    Given a booking exists in RECEIVED state
    When I click the update state button for the booking
    And I select a new state "ASSIGNED"
    And I confirm the state update
    Then the booking state should be updated to "ASSIGNED"

  Scenario: Filter bookings by state
    Given bookings exist in different states
    When I select state "IN_PROGRESS" from the state filter
    And I click refresh button
    Then I should only see bookings in IN_PROGRESS state