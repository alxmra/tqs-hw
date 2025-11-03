Feature: Website Navigation
  As a user
  I want to navigate between different pages
  So that I can access different functionalities

  Background:
    Given the application is running

  Scenario: Navigate from index to citizen portal
    When I navigate to the home page
    Then I should be redirected to the citizen portal

  Scenario: Navigate between pages using navigation menu
    When I navigate to the citizen portal
    And I click on the staff dashboard link
    Then I should be on the staff dashboard page

  Scenario: Navigate to check booking page
    When I navigate to the citizen portal
    And I click on the check booking link
    Then I should be on the check booking page