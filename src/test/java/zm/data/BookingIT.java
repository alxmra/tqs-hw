package zm.data;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
class BookingIT {

    @LocalServerPort
    private int port;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        baseUrl = "http://localhost:" + port + "/api";
    }

    @Test
    void testCreateBookingSuccessfully() {
        Map<String, Object> bookingRequest = createValidBookingRequest();

        given()
            .contentType(ContentType.JSON)
            .body(bookingRequest)
        .when()
            .post(baseUrl + "/bookings")
        .then()
            .statusCode(201)
            .body("token", notNullValue())
            .body("token", not(emptyString()))
            .body("message", containsString("success"));
    }

    @Test
    void testCreateBookingWithInvalidMunicipality() {
        Map<String, Object> bookingRequest = createValidBookingRequest();
        bookingRequest.put("municipality", "InvalidCity123");

        given()
            .contentType(ContentType.JSON)
            .body(bookingRequest)
        .when()
            .post(baseUrl + "/bookings")
        .then()
            .statusCode(400);
    }

    @Test
    void testCreateBookingWithPastDate() {
        Map<String, Object> bookingRequest = createValidBookingRequest();
        bookingRequest.put("date", LocalDate.now().minusDays(1).toString());

        given()
            .contentType(ContentType.JSON)
            .body(bookingRequest)
        .when()
            .post(baseUrl + "/bookings")
        .then()
            .statusCode(400);
    }

    @Test
    void testCreateBookingWithEmptyItems() {
        Map<String, Object> bookingRequest = createValidBookingRequest();
        bookingRequest.put("items", List.of());

        given()
            .contentType(ContentType.JSON)
            .body(bookingRequest)
        .when()
            .post(baseUrl + "/bookings")
        .then()
            .statusCode(400);
    }

    @Test
    void testCreateBookingWithNullMunicipality() {
        Map<String, Object> bookingRequest = createValidBookingRequest();
        bookingRequest.put("municipality", null);

        given()
            .contentType(ContentType.JSON)
            .body(bookingRequest)
        .when()
            .post(baseUrl + "/bookings")
        .then()
            .statusCode(400);
    }

    @Test
    void testCheckBookingWithValidToken() {
        String token = createBookingAndGetToken();

        given()
            .pathParam("token", token)
        .when()
            .get(baseUrl + "/bookings/{token}")
        .then()
            .statusCode(200)
            .body("token", equalTo(token))
            .body("municipality", notNullValue())
            .body("items", notNullValue())
            .body("currentState.state", equalTo("RECEIVED"));
    }

    @Test
    void testCheckBookingWithInvalidToken() {
        given()
            .pathParam("token", "INVALID_TOKEN_12345")
        .when()
            .get(baseUrl + "/bookings/{token}")
        .then()
            .statusCode(404);
    }

    @Test
    void testCancelBookingSuccessfully() {
        String token = createBookingAndGetToken();

        given()
            .pathParam("token", token)
        .when()
            .delete(baseUrl + "/bookings/{token}")
        .then()
            .statusCode(204);
    }

    @Test
    void testCancelBookingWithInvalidToken() {
        given()
            .pathParam("token", "NONEXISTENT_TOKEN")
        .when()
            .delete(baseUrl + "/bookings/{token}")
        .then()
            .statusCode(404);
    }

    @Test
    void testCancelAlreadyCancelledBooking() {
        String token = createBookingAndGetToken();

        given().pathParam("token", token).delete(baseUrl + "/bookings/{token}");

        given()
            .pathParam("token", token)
        .when()
            .delete(baseUrl + "/bookings/{token}")
        .then()
            .statusCode(404);
    }

    @Test
    void testGetAllBookings() {
        createBookingAndGetToken();
        createBookingAndGetToken();

        given()
        .when()
            .get(baseUrl + "/staff/bookings")
        .then()
            .statusCode(200)
            .body("$", hasSize(greaterThanOrEqualTo(2)));
    }

    @Test
    void testGetAllBookingsEmpty() {
        given()
        .when()
            .get(baseUrl + "/staff/bookings")
        .then()
            .statusCode(200)
            .body("$", isA(List.class));
    }

    @Test
    void testChangeBookingState() {
        String token = createBookingAndGetToken();

        given()
            .pathParam("token", token)
        .when()
            .patch(baseUrl + "/bookings/{token}/state")
        .then()
            .statusCode(204);

        given()
            .pathParam("token", token)
        .when()
            .get(baseUrl + "/bookings/{token}")
        .then()
            .statusCode(200)
            .body("currentState.state", not(equalTo("RECEIVED")));
    }

    @Test
    void testChangeStateWithInvalidToken() {
        given()
            .pathParam("token", "INVALID_TOKEN")
        .when()
            .patch(baseUrl + "/bookings/{token}/state")
        .then()
            .statusCode(404);
    }

    @Test
    void testGetBookingsByState() {
        given()
            .pathParam("state", "RECEIVED")
        .when()
            .get(baseUrl + "/bookings/state/{state}")
        .then()
            .statusCode(200)
            .body("$", isA(List.class));
    }

    @Test
    void testGetBookingsByMunicipality() {
        createBookingAndGetToken();

        given()
            .pathParam("municipality", "Aveiro")
        .when()
            .get(baseUrl + "/municipalities/{municipality}")
        .then()
            .statusCode(200)
            .body("$", isA(List.class))
            .body("$", hasSize(greaterThanOrEqualTo(1)));
    }

    @Test
    void testGetBookingsByMunicipalityEmpty() {
        given()
            .pathParam("municipality", "UnusedCity")
        .when()
            .get(baseUrl + "/municipalities/{municipality}")
        .then()
            .statusCode(200)
            .body("$", hasSize(0));
    }

    @Test
    void testCreateBookingWithMalformedJson() {
        given()
            .contentType(ContentType.JSON)
            .body("\"abc\"")
        .when()
            .post(baseUrl + "/bookings")
        .then()
            .statusCode(400);
    }

    @Test
    void testCreateBookingWithMissingRequiredFields() {
        Map<String, Object> incompleteRequest = new HashMap<>();
        incompleteRequest.put("municipality", "Aveiro");

        given()
            .contentType(ContentType.JSON)
            .body(incompleteRequest)
        .when()
            .post(baseUrl + "/bookings")
        .then()
            .statusCode(400);
    }

    @Test
    void testFullBookingLifecycle() {
        String token = createBookingAndGetToken();

        given().pathParam("token", token).get(baseUrl + "/bookings/{token}")
            .then().statusCode(200).body("currentState.state", equalTo("RECEIVED"));

        given().pathParam("token", token).patch(baseUrl + "/bookings/{token}/state")
            .then().statusCode(204);

        given().pathParam("token", token).get(baseUrl + "/bookings/{token}")
            .then().statusCode(200).body("currentState.state", not(equalTo("RECEIVED")));

        given().pathParam("token", token).delete(baseUrl + "/bookings/{token}")
            .then().statusCode(anyOf(equalTo(204), equalTo(404)));
    }

    @Test
    void testConcurrentBookingCreation() {
        Map<String, Object> request1 = createValidBookingRequest();
        Map<String, Object> request2 = createValidBookingRequest();

        String token1 = given().contentType(ContentType.JSON).body(request1)
            .post(baseUrl + "/bookings").then().statusCode(201)
            .extract().path("token");

        String token2 = given().contentType(ContentType.JSON).body(request2)
            .post(baseUrl + "/bookings").then().statusCode(201)
            .extract().path("token");

        assertNotEquals(token1, token2);
    }

    @Test
    void testBookingWithSpecialCharactersInItems() {
        Map<String, Object> bookingRequest = createValidBookingRequest();
        List<Map<String, String>> specialItems = Arrays.asList(
            createItemMap("Sofá & Cadeiras", "Móveis antigos - €500"),
            createItemMap("TV 50\"", "Televisão quebrada (220V)")
        );
        bookingRequest.put("items", specialItems);

        given()
            .contentType(ContentType.JSON)
            .body(bookingRequest)
        .when()
            .post(baseUrl + "/bookings")
        .then()
            .statusCode(201)
            .body("token", notNullValue());
    }

    @Test
    void testBookingStateHistory() {
        String token = createBookingAndGetToken();

        given().pathParam("token", token).patch(baseUrl + "/bookings/{token}/state");
        given().pathParam("token", token).patch(baseUrl + "/bookings/{token}/state");

        given()
            .pathParam("token", token)
        .when()
            .get(baseUrl + "/bookings/{token}")
        .then()
            .statusCode(200)
            .body("previousStates", hasSize(greaterThanOrEqualTo(1)));
    }

    @Test
    void testCreateBookingWithMaxItems() {
        Map<String, Object> bookingRequest = createValidBookingRequest();
        List<Map<String, String>> maxItems = Arrays.asList(
            createItemMap("Item1", "Description1"),
            createItemMap("Item2", "Description2"),
            createItemMap("Item3", "Description3"),
            createItemMap("Item4", "Description4"),
            createItemMap("Item5", "Description5"),
            createItemMap("Item6", "Description6"),
            createItemMap("Item7", "Description7"),
            createItemMap("Item8", "Description8"),
            createItemMap("Item9", "Description9"),
            createItemMap("Item10", "Description10")
        );
        bookingRequest.put("items", maxItems);

        given()
            .contentType(ContentType.JSON)
            .body(bookingRequest)
        .when()
            .post(baseUrl + "/bookings")
        .then()
            .statusCode(anyOf(equalTo(201), equalTo(400)));
    }

    @Test
    void testBookingWithFutureDate() {
        Map<String, Object> bookingRequest = createValidBookingRequest();
        bookingRequest.put("date", LocalDate.now().plusDays(30).toString());

        given()
            .contentType(ContentType.JSON)
            .body(bookingRequest)
        .when()
            .post(baseUrl + "/bookings")
        .then()
            .statusCode(201)
            .body("token", notNullValue());
    }

    @Test
    void testResponseContentType() {
        String token = createBookingAndGetToken();

        given()
            .pathParam("token", token)
        .when()
            .get(baseUrl + "/bookings/{token}")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    private Map<String, Object> createValidBookingRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("date", LocalDate.now().plusDays(7).toString());
        request.put("approxTimeSlot", LocalTime.of(10, 0).toString());
        request.put("municipality", "Aveiro");
        
        List<Map<String, String>> items = Arrays.asList(
            createItemMap("Mattress", "Old king-size mattress"),
            createItemMap("Sofa", "Leather sofa")
        );
        request.put("items", items);
        
        return request;
    }

    private Map<String, String> createItemMap(String name, String description) {
        Map<String, String> item = new HashMap<>();
        item.put("name", name);
        item.put("description", description);
        return item;
    }

    private String createBookingAndGetToken() {
        Map<String, Object> bookingRequest = createValidBookingRequest();

        return given()
            .contentType(ContentType.JSON)
            .body(bookingRequest)
        .when()
            .post(baseUrl + "/bookings")
        .then()
            .statusCode(201)
            .extract()
            .path("token");
    }

    private void assertNotEquals(String token1, String token2) {
        if (token1.equals(token2)) {
            throw new AssertionError("Tokens should not be equal");
        }
    }
}