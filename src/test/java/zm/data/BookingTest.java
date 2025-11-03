package zm.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BookingTest {

    private Booking booking;
    private List<Item> items;

    @BeforeEach
    void setUp() {
        items = new ArrayList<>();
        items.add(new Item("Mattress", "Old king-size mattress"));
        items.add(new Item("Refrigerator", "Broken fridge"));
        
        booking = new Booking(
            LocalDate.now().plusDays(5),
            LocalTime.of(10, 0),
            items,
            "Aveiro"
        );
    }

    @Test
    void testBookingCreation() {
        assertNotNull(booking);
        assertNotNull(booking.getToken());
        assertEquals(State.RECEIVED, booking.getCurrentState().getState());
        assertEquals("Aveiro", booking.getMunicipality());
        assertEquals(2, booking.getItems().size());
    }

    @Test
    void testChangeStateToAssigned() {
        boolean result = booking.changeState(State.ASSIGNED);
        
        assertTrue(result);
        assertEquals(State.ASSIGNED, booking.getCurrentState().getState());
        assertEquals(1, booking.getPreviousStates().size());
    }

    @Test
    void testChangeStateToInProgress() {
        booking.changeState(State.ASSIGNED);
        boolean result = booking.changeState(State.IN_PROGRESS);
        
        assertTrue(result);
        assertEquals(State.IN_PROGRESS, booking.getCurrentState().getState());
    }

    @Test
    void testChangeStateToFinished() {
        booking.changeState(State.ASSIGNED);
        booking.changeState(State.IN_PROGRESS);
        boolean result = booking.changeState(State.FINISHED);
        
        assertTrue(result);
        assertEquals(State.FINISHED, booking.getCurrentState().getState());
    }

    @Test
    void testChangeStateToCancelled() {
        boolean result = booking.changeState(State.CANCELLED);
        
        assertTrue(result);
        assertEquals(State.CANCELLED, booking.getCurrentState().getState());
    }

    @Test
    void testChangeStateInvalidTransition() {
        booking.changeState(State.ASSIGNED);
        boolean result = booking.changeState(State.RECEIVED);
        
        assertFalse(result);
        assertEquals(State.ASSIGNED, booking.getCurrentState().getState());
    }

    @Test
    void testCannotChangeStateAfterFinished() {
        booking.changeState(State.ASSIGNED);
        booking.changeState(State.IN_PROGRESS);
        booking.changeState(State.FINISHED);
        
        boolean result = booking.changeState(State.ASSIGNED);
        
        assertFalse(result);
        assertEquals(State.FINISHED, booking.getCurrentState().getState());
    }

    @Test
    void testCannotChangeStateAfterCancelled() {
        booking.changeState(State.CANCELLED);
        boolean result = booking.changeState(State.ASSIGNED);
        
        assertFalse(result);
        assertEquals(State.CANCELLED, booking.getCurrentState().getState());
    }

    @Test
    void testPreviousStatesTracking() {
        booking.changeState(State.ASSIGNED);
        booking.changeState(State.IN_PROGRESS);
        
        assertEquals(2, booking.getPreviousStates().size());
        assertEquals(State.RECEIVED, booking.getPreviousStates().get(0).getState());
        assertEquals(State.ASSIGNED, booking.getPreviousStates().get(1).getState());
    }

    @Test
    void testTokenIsUnique() {
        Booking booking2 = new Booking(
            LocalDate.now().plusDays(3),
            LocalTime.of(14, 0),
            items,
            "Porto"
        );
        
        assertNotEquals(booking.getToken(), booking2.getToken());
    }

    @Test
    void testBookingWithPastDate() {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        
        assertThrows(IllegalArgumentException.class, () -> 
            new Booking(pastDate, LocalTime.of(10, 0), items, "Lisboa")
        );
    }

    @Test
    void testBookingWithEmptyItems() {
        assertThrows(IllegalArgumentException.class, () -> 
            new Booking(LocalDate.now().plusDays(1), LocalTime.of(10, 0), new ArrayList<>(), "Braga")
        );
    }

    @Test
    void testBookingWithNullMunicipality() {
        assertThrows(IllegalArgumentException.class, () -> 
            new Booking(LocalDate.now().plusDays(1), LocalTime.of(10, 0), items, null)
        );
    }

    @Test
    void testChangeStateToRemoved() {
        booking.changeState(State.ASSIGNED);
        boolean result = booking.changeState(State.REMOVED);
        
        assertTrue(result);
        assertEquals(State.REMOVED, booking.getCurrentState().getState());
    }

    @Test
    void testCannotChangeStateAfterRemoved() {
        booking.changeState(State.REMOVED);
        boolean result = booking.changeState(State.ASSIGNED);
        
        assertFalse(result);
        assertEquals(State.REMOVED, booking.getCurrentState().getState());
    }
}