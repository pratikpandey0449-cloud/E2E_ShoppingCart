Feature: Demo Webshop end-to-end purchase

  @e2e
  Scenario: User completes purchase flow from login to logout
    Given user launches the browser and opens demo webshop
    When user logs in with valid credentials
    And user searches for a product and adds first result to cart
    Then cart should contain the selected product
    When user proceeds to checkout and completes billing shipping and payment steps
    Then order should be placed successfully
    And user logs out successfully
