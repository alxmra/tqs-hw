package zm.boundary;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import zm.data.Booking;
import zm.data.Item;
import zm.data.State;
import zm.services.BookingService;
import zm.services.MunicipalityProvider;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

@WebMvcTest(BookingController.class)
class BookingControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MunicipalityProvider municipalityProvider;

    @MockBean
    private BookingService bookingService;

    private List<Item> items;
    private Booking sampleBooking;

    @BeforeEach
    void setUp() {
        items = Arrays.asList(
            new Item("Mattress", "Old king-size mattress"),
            new Item("Sofa", "Leather sofa")
        );

        sampleBooking = new Booking(
            LocalDate.now().plusDays(5),
            LocalTime.of(10, 0),
            items,
            "Aveiro"
        );
    }

    @Test
    void testBookSuccess() throws Exception {
        BookingRequest request = new BookingRequest(
            LocalDate.now().plusDays(5),
            LocalTime.of(10, 0),
            items,
            "Aveiro"
        );

        when(bookingService.book(any(LocalDate.class), any(LocalTime.class), anyList(), anyString()))
            .thenReturn("ABC123TOKEN");

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").value("ABC123TOKEN"))
            .andExpect(jsonPath("$.message").exists());

        verify(bookingService).book(any(LocalDate.class), any(LocalTime.class), anyList(), anyString());
    }

    @Test
    void testBookWithInvalidMunicipality() throws Exception {
        BookingRequest request = new BookingRequest(
            LocalDate.now().plusDays(5),
            LocalTime.of(10, 0),
            items,
            "InvalidCity"
        );

        when(bookingService.book(any(LocalDate.class), any(LocalTime.class), anyList(), anyString()))
            .thenThrow(new IllegalArgumentException("Invalid municipality"));

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(bookingService).book(any(LocalDate.class), any(LocalTime.class), anyList(), anyString());
    }

    @Test
    void testBookWithPastDate() throws Exception {
        BookingRequest request = new BookingRequest(
            LocalDate.now().minusDays(1),
            LocalTime.of(10, 0),
            items,
            "Aveiro"
        );

        when(bookingService.book(any(LocalDate.class), any(LocalTime.class), anyList(), anyString()))
            .thenThrow(new IllegalArgumentException("Date must be in the future"));

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testBookWithEmptyItems() throws Exception {
        BookingRequest request = new BookingRequest(
            LocalDate.now().plusDays(5),
            LocalTime.of(10, 0),
            List.of(),
            "Aveiro"
        );

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(bookingService, never()).book(any(), any(), any(), any());
    }

    @Test
    void testBookWithNullMunicipality() throws Exception {
        BookingRequest request = new BookingRequest(
            LocalDate.now().plusDays(5),
            LocalTime.of(10, 0),
            items,
            null
        );

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(bookingService, never()).book(any(), any(), any(), any());
    }

    @Test
    void testCancelSuccess() throws Exception {
        when(bookingService.cancel("TOKEN123")).thenReturn(true);

        mockMvc.perform(delete("/api/bookings/TOKEN123"))
            .andExpect(status().isNoContent());

        verify(bookingService).cancel("TOKEN123");
    }

    @Test
    void testCancelNotFound() throws Exception {
        when(bookingService.cancel("INVALID")).thenReturn(false);

        mockMvc.perform(delete("/api/bookings/INVALID"))
            .andExpect(status().isNotFound());

        verify(bookingService).cancel("INVALID");
    }

    @Test
    void testCancelAlreadyCancelled() throws Exception {
        when(bookingService.cancel("TOKEN123")).thenReturn(false);

        mockMvc.perform(delete("/api/bookings/TOKEN123"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testCheckBookingSuccess() throws Exception {
        when(bookingService.check("TOKEN456")).thenReturn(sampleBooking);

        mockMvc.perform(get("/api/bookings/TOKEN456"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.municipality").value("Aveiro"))
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items", hasSize(2)))
            .andExpect(jsonPath("$.currentState.state").value("RECEIVED"));

        verify(bookingService).check("TOKEN456");
    }

    @Test
    void testCheckBookingNotFound() throws Exception {
        when(bookingService.check("NOTFOUND")).thenReturn(null);

        mockMvc.perform(get("/api/bookings/NOTFOUND"))
            .andExpect(status().isNotFound());

        verify(bookingService).check("NOTFOUND");
    }

    @Test
    void testGetAllBookings() throws Exception {
        List<Booking> bookings = List.of(sampleBooking);
        when(bookingService.getAllBookings()).thenReturn(bookings);

        mockMvc.perform(get("/api/staff/bookings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].municipality").value("Aveiro"));

        verify(bookingService).getAllBookings();
    }

    @Test
    void testGetAllBookingsEmpty() throws Exception {
        when(bookingService.getAllBookings()).thenReturn(List.of());

        mockMvc.perform(get("/api/staff/bookings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(0)));

        verify(bookingService).getAllBookings();
    }

    @Test
    void testModifyStateSuccess() throws Exception {
        when(bookingService.changeState(eq("TOKEN789"), any(State.class))).thenReturn(true);

        mockMvc.perform(patch("/api/bookings/TOKEN789/state"))
            .andExpect(status().isNoContent());

        verify(bookingService).changeState(eq("TOKEN789"), any(State.class));
    }

    @Test
    void testModifyStateNotFound() throws Exception {
        when(bookingService.changeState(eq("NOTFOUND"), any(State.class))).thenReturn(false);

        mockMvc.perform(patch("/api/bookings/NOTFOUND/state"))
            .andExpect(status().isNotFound());

        verify(bookingService).changeState(eq("NOTFOUND"), any(State.class));
    }

    @Test
    void testModifyStateInvalidTransition() throws Exception {
        when(bookingService.changeState(eq("TOKEN123"), any(State.class))).thenReturn(false);

        mockMvc.perform(patch("/api/bookings/TOKEN123/state"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testGetBookingsByState() throws Exception {
        List<Booking> bookings = Arrays.asList(sampleBooking);
        when(bookingService.getBookingsByState(State.ASSIGNED)).thenReturn(bookings);

        mockMvc.perform(get("/api/bookings/state/ASSIGNED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(1)));

        verify(bookingService).getBookingsByState(State.ASSIGNED);
    }

    @Test
    void testGetBookingsByMunicipality() throws Exception {
        List<Booking> bookings = Arrays.asList(sampleBooking);
        when(bookingService.getBookingsByMunicipality("Aveiro")).thenReturn(bookings);

        mockMvc.perform(get("/api/municipalities/Aveiro"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].municipality").value("Aveiro"));

        verify(bookingService).getBookingsByMunicipality("Aveiro");
    }

    @Test
    void testGetBookingsByMunicipalityEmpty() throws Exception {
        when(bookingService.getBookingsByMunicipality("Porto")).thenReturn(List.of());

        mockMvc.perform(get("/api/municipalities/Porto"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testBookWithInvalidJson() throws Exception {
        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"not-a-date\"}"))
            .andExpect(status().isBadRequest());

        verify(bookingService, never()).book(any(), any(), any(), any());
    }

    @Test
    void testBookWithMissingFields() throws Exception {
        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        verify(bookingService, never()).book(any(), any(), any(), any());
    }

    @Test
    void testBookCapacityExceeded() throws Exception {
        BookingRequest request = new BookingRequest(
            LocalDate.now().plusDays(5),
            LocalTime.of(10, 0),
            items,
            "Aveiro"
        );

        when(bookingService.book(any(LocalDate.class), any(LocalTime.class), anyList(), anyString()))
            .thenThrow(new IllegalStateException("Capacity exceeded"));

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void testGetBookingWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/bookings/"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testCancelWithEmptyToken() throws Exception {
        mockMvc.perform(delete("/api/bookings/"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testModifyStateWithEmptyToken() throws Exception {
        mockMvc.perform(patch("/api/bookings//state"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testBookWithNullDate() throws Exception {
        BookingRequest request = new BookingRequest(
            null,
            LocalTime.of(10, 0),
            items,
            "Aveiro"
        );

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(bookingService, never()).book(any(), any(), any(), any());
    }

    @Test
    void testBookWithNullTimeSlot() throws Exception {
        BookingRequest request = new BookingRequest(
            LocalDate.now().plusDays(5),
            null,
            items,
            "Aveiro"
        );

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(bookingService, never()).book(any(), any(), any(), any());
    }

    @Test
    void testBookWithEmptyMunicipality() throws Exception {
        BookingRequest request = new BookingRequest(
            LocalDate.now().plusDays(5),
            LocalTime.of(10, 0),
            items,
            "   "
        );

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(bookingService, never()).book(any(), any(), any(), any());
    }

    @Test
    void testBookWithNullItems() throws Exception {
        BookingRequest request = new BookingRequest(
            LocalDate.now().plusDays(5),
            LocalTime.of(10, 0),
            null,
            "Aveiro"
        );

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(bookingService, never()).book(any(), any(), any(), any());
    }

    @Test
    void testBookWithEmptyToken() throws Exception {
        BookingRequest request = new BookingRequest(
            LocalDate.now().plusDays(5),
            LocalTime.of(10, 0),
            items,
            "Aveiro"
        );

        when(bookingService.book(any(LocalDate.class), any(LocalTime.class), anyList(), anyString()))
            .thenReturn("");

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testBookWithNullTokenReturned() throws Exception {
        BookingRequest request = new BookingRequest(
            LocalDate.now().plusDays(5),
            LocalTime.of(10, 0),
            items,
            "Aveiro"
        );

        when(bookingService.book(any(LocalDate.class), any(LocalTime.class), anyList(), anyString()))
            .thenReturn(null);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testBookWithUnexpectedException() throws Exception {
        BookingRequest request = new BookingRequest(
            LocalDate.now().plusDays(5),
            LocalTime.of(10, 0),
            items,
            "Aveiro"
        );

        when(bookingService.book(any(LocalDate.class), any(LocalTime.class), anyList(), anyString()))
            .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void testCheckBookingWithEmptyToken() throws Exception {
        mockMvc.perform(get("/api/bookings/   "))
            .andExpect(status().isNotFound());

        verify(bookingService, never()).check(any());
    }

    @Test
    void testCheckBookingWithException() throws Exception {
        when(bookingService.check("TOKEN456")).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/bookings/TOKEN456"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void testCancelWithWhitespaceToken() throws Exception {
        mockMvc.perform(delete("/api/bookings/   "))
            .andExpect(status().isNotFound());

        verify(bookingService, never()).cancel(any());
    }

    @Test
    void testCancelWithException() throws Exception {
        when(bookingService.cancel("TOKEN123")).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(delete("/api/bookings/TOKEN123"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void testModifyStateWithWhitespaceToken() throws Exception {
        mockMvc.perform(patch("/api/bookings/   /state"))
            .andExpect(status().isNotFound());

        verify(bookingService, never()).changeState(any(), any());
    }

    @Test
    void testModifyStateWithRequestBody() throws Exception {
        StateUpdateRequest stateRequest = new StateUpdateRequest(State.FINISHED);
        when(bookingService.changeState(eq("TOKEN789"), eq(State.FINISHED))).thenReturn(true);

        mockMvc.perform(patch("/api/bookings/TOKEN789/state")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stateRequest)))
            .andExpect(status().isNoContent());

        verify(bookingService).changeState(eq("TOKEN789"), eq(State.FINISHED));
    }

    @Test
    void testModifyStateWithException() throws Exception {
        when(bookingService.changeState(eq("TOKEN789"), any(State.class)))
            .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(patch("/api/bookings/TOKEN789/state"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void testModifyWithoutToken() throws Exception {
        mockMvc.perform(patch("/api/bookings/state"))
            .andExpect(status().isNotFound());

        verify(bookingService, never()).changeState(any(), any());
    }

    @Test
    void testGetAllBookingsWithException() throws Exception {
        when(bookingService.getAllBookings()).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/staff/bookings"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetBookingsByStateInvalidState() throws Exception {
        mockMvc.perform(get("/api/bookings/state/INVALID_STATE"))
            .andExpect(status().isBadRequest());

        verify(bookingService, never()).getBookingsByState(any());
    }

    @Test
    void testGetBookingsByStateWithException() throws Exception {
        when(bookingService.getBookingsByState(State.ASSIGNED))
            .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/bookings/state/ASSIGNED"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetBookingsByMunicipalityWithException() throws Exception {
        when(bookingService.getBookingsByMunicipality("Aveiro"))
            .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/municipalities/Aveiro"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetMunicipalities() throws Exception {
        List<String> municipalities = Arrays.asList("Aveiro", "Porto", "Lisboa");
        when(municipalityProvider.getMunicipalities()).thenReturn(municipalities);

        mockMvc.perform(get("/api/municipalities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0]").value("Aveiro"));

        verify(municipalityProvider).getMunicipalities();
    }

    @Test
    void testGetMunicipalitiesWithException() throws Exception {
        when(municipalityProvider.getMunicipalities())
            .thenThrow(new RuntimeException("Service unavailable"));

        mockMvc.perform(get("/api/municipalities"))
            .andExpect(status().isInternalServerError());
    }

    static class StateUpdateRequest {
        public State state;

        @SuppressWarnings("unused")
        public StateUpdateRequest() {}

        public StateUpdateRequest(State state) {
            this.state = state;
        }
    }

    static class BookingRequest {
        public LocalDate date;
        public LocalTime approxTimeSlot;
        public List<Item> items;
        public String municipality;

        @SuppressWarnings("unused")
        public BookingRequest() {}

        public BookingRequest(LocalDate date, LocalTime approxTimeSlot, List<Item> items, String municipality) {
            this.date = date;
            this.approxTimeSlot = approxTimeSlot;
            this.items = items;
            this.municipality = municipality;
        }
    }
}