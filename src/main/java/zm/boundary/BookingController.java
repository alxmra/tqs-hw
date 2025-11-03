package zm.boundary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import zm.data.Booking;
import zm.data.Item;
import zm.data.State;
import zm.services.BookingService;
import zm.services.MunicipalityProvider;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);
    private final BookingService bookingService;
    private final MunicipalityProvider municipalityProvider;

    public BookingController(BookingService bookingService, MunicipalityProvider municipalityProvider) {
        this.bookingService = bookingService;
        this.municipalityProvider = municipalityProvider;
    }

    @PostMapping("/bookings")
    public ResponseEntity<Object> book(@RequestBody BookingRequest request) {
        logger.info("Received booking request for municipality: {}", request.getMunicipality());
        
        if (request.getMunicipality() == null || request.getMunicipality().trim().isEmpty()) {
            logger.warn("Booking request rejected: municipality is null or empty");
            return ResponseEntity.badRequest().body("Municipality is required");
        }
        
        if (request.getItems() == null || request.getItems().isEmpty()) {
            logger.warn("Booking request rejected: items list is empty");
            return ResponseEntity.badRequest().body("At least one item is required");
        }
        
        if (request.getDate() == null) {
            logger.warn("Booking request rejected: date is null");
            return ResponseEntity.badRequest().body("Date is required");
        }
        
        if (request.getApproxTimeSlot() == null) {
            logger.warn("Booking request rejected: time slot is null");
            return ResponseEntity.badRequest().body("Time slot is required");
        }
        
        try {
            String token = bookingService.book(
                request.getDate(), 
                request.getApproxTimeSlot(), 
                request.getItems(), 
                request.getMunicipality()
            );
            
            if (token == null || token.isEmpty()) {
                logger.error("Booking failed: service returned empty token");
                return ResponseEntity.badRequest().body("Booking failed: invalid municipality or service unavailable");
            }
            
            logger.info("Booking created successfully with token: {}", token);
            BookingResponse response = new BookingResponse(token, "Booking created successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Booking request rejected: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("Booking request rejected due to capacity: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during booking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred");
        }
    }

    // Booking details by token
    @GetMapping("/bookings/{token}")
    public ResponseEntity<Object> check(@PathVariable String token) {
        logger.info("Checking booking with token: {}", token);
        
        if (token == null || token.trim().isEmpty()) {
            logger.warn("Check request rejected: token is empty");
            return ResponseEntity.notFound().build();
        }
        
        try {
            Booking booking = bookingService.check(token);
            if (booking == null) {
                logger.warn("Booking not found for token: {}", token);
                return ResponseEntity.notFound().build();
            }
            
            logger.info("Booking found for token: {}", token);
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            logger.error("Error checking booking with token: {}", token, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving booking");
        }
    }

    @DeleteMapping("/bookings/{token}")
    public ResponseEntity<Void> cancel(@PathVariable String token) {
        logger.info("Cancelling booking with token: {}", token);
        
        if (token == null || token.trim().isEmpty()) {
            logger.warn("Cancel request rejected: token is empty");
            return ResponseEntity.notFound().build();
        }
        
        try {
            boolean cancelled = bookingService.cancel(token);
            if (cancelled) {
                logger.info("Booking cancelled successfully: {}", token);
                return ResponseEntity.noContent().build();
            } else {
                logger.warn("Booking not found or already cancelled: {}", token);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error cancelling booking with token: {}", token, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/bookings/{token}/state")
    public ResponseEntity<Void> modify(@PathVariable String token, @RequestBody(required = false) StateUpdateRequest request) {
        logger.info("Modifying state for booking with token: {}", token);
        
        if (token == null || token.trim().isEmpty()) {
            logger.warn("Modify request rejected: token is empty");
            return ResponseEntity.notFound().build();
        }
        
        // Default to ASSIGNED if no state provided (for backwards compatibility)
        State newState = State.ASSIGNED;
        if (request != null && request.getState() != null) {
            newState = request.getState();
        }
        
        try {
            boolean updated = bookingService.changeState(token, newState);
            if (updated) {
                logger.info("Booking state updated successfully: {} to {}", token, newState);
                return ResponseEntity.noContent().build();
            } else {
                logger.warn("Booking not found or state transition invalid: {}", token);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error updating booking state for token: {}", token, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/bookings/state")
    public ResponseEntity<Void> modifyWithoutToken() {
        logger.warn("Modify request rejected: token is missing");
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/staff/bookings")
    public ResponseEntity<List<Booking>> getAllBookings() {
        logger.info("Retrieving all bookings");
        
        try {
            List<Booking> bookings = bookingService.getAllBookings();
            logger.info("Retrieved {} bookings", bookings.size());
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            logger.error("Error retrieving all bookings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/bookings/state/{state}")
    public ResponseEntity<Object> getBookingsByState(@PathVariable String state) {
        logger.info("Retrieving bookings with state: {}", state);
        
        try {
            State filterState = State.valueOf(state.toUpperCase());
            List<Booking> bookings = bookingService.getBookingsByState(filterState);
            logger.info("Retrieved {} bookings with state {}", bookings.size(), state);
            return ResponseEntity.ok(bookings);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid state provided: {}", state);
            return ResponseEntity.badRequest().body("Invalid state: " + state);
        } catch (Exception e) {
            logger.error("Error retrieving bookings by state: {}", state, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/municipalities/{municipality}")
    public ResponseEntity<List<Booking>> getBookingsByMunicipality(@PathVariable String municipality) {
        logger.info("Retrieving bookings for municipality: {}", municipality);
        
        try {
            List<Booking> bookings = bookingService.getBookingsByMunicipality(municipality);
            logger.info("Retrieved {} bookings for municipality {}", bookings.size(), municipality);
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            logger.error("Error retrieving bookings by municipality: {}", municipality, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/municipalities")
    public ResponseEntity<List<String>> getMunicipalities() {
        logger.info("Retrieving municipalities list");
        
        try {
            List<String> municipalities = municipalityProvider.getMunicipalities();
            logger.info("Retrieved {} municipalities", municipalities.size());
            return ResponseEntity.ok(municipalities);
        } catch (Exception e) {
            logger.error("Error retrieving municipalities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public static class BookingRequest {
        private LocalDate date;
        private LocalTime approxTimeSlot;
        private List<Item> items;
        private String municipality;

        public BookingRequest() {}

        public BookingRequest(LocalDate date, LocalTime approxTimeSlot, List<Item> items, String municipality) {
            this.date = date;
            this.approxTimeSlot = approxTimeSlot;
            this.items = items;
            this.municipality = municipality;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public LocalTime getApproxTimeSlot() {
            return approxTimeSlot;
        }

        public void setApproxTimeSlot(LocalTime approxTimeSlot) {
            this.approxTimeSlot = approxTimeSlot;
        }

        public List<Item> getItems() {
            return items;
        }

        public void setItems(List<Item> items) {
            this.items = items;
        }

        public String getMunicipality() {
            return municipality;
        }

        public void setMunicipality(String municipality) {
            this.municipality = municipality;
        }
    }

    public static class BookingResponse {
        private String token;
        private String message;

        public BookingResponse() {}

        public BookingResponse(String token, String message) {
            this.token = token;
            this.message = message;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class StateUpdateRequest {
        private State state;

        public StateUpdateRequest() {}

        public StateUpdateRequest(State state) {
            this.state = state;
        }

        public State getState() {
            return state;
        }

        public void setState(State state) {
            this.state = state;
        }
    }
}