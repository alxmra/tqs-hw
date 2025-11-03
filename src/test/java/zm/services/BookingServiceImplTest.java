package zm.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import zm.data.Booking;
import zm.data.Item;
import zm.data.State;
import zm.data.BookingRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private MunicipalityProvider municipalityProvider;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private List<Item> items;
    private LocalDate futureDate;
    private LocalTime timeSlot;

    @BeforeEach
    void setUp() {
        items = new ArrayList<>();
        items.add(new Item("Mattress", "Old mattress"));
        items.add(new Item("Sofa", "Leather sofa"));
        
        futureDate = LocalDate.now().plusDays(7);
        timeSlot = LocalTime.of(10, 0);
    }

    @Test
    void testBookWithValidMunicipality() {
        when(municipalityProvider.isValid("Aveiro")).thenReturn(true);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        String token = bookingService.book(futureDate, timeSlot, items, "Aveiro");
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        verify(municipalityProvider).isValid("Aveiro");
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void testBookWithInvalidMunicipality() {
        when(municipalityProvider.isValid("InvalidCity")).thenReturn(false);
        
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.book(futureDate, timeSlot, items, "InvalidCity");
        });
        
        verify(municipalityProvider).isValid("InvalidCity");
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testBookWithBlacklistedMunicipality() {
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.book(futureDate, timeSlot, items, "Porto");
        });
        
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testBookWithPastDate() {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.book(pastDate, timeSlot, items, "Aveiro");
        });
        
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testBookWithEmptyItems() {
        when(municipalityProvider.isValid("Aveiro")).thenReturn(true);
        
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.book(futureDate, timeSlot, new ArrayList<>(), "Aveiro");
        });
        
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testBookWithTooManyItems() {
        List<Item> tooManyItems = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tooManyItems.add(new Item("Item" + i, "Description"));
        }
        
        assertThrows(IllegalArgumentException.class, () -> 
            bookingService.book(futureDate, timeSlot, tooManyItems, "Aveiro")
        );
        
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testBookWhenCapacityExceeded() {
        when(municipalityProvider.isValid("Aveiro")).thenReturn(true);
        when(bookingRepository.findByDateAndApproxTimeSlotAndMunicipality(futureDate, timeSlot, "Aveiro"))
            .thenReturn(createManyBookings(50));
        
        assertThrows(IllegalStateException.class, () -> {
            bookingService.book(futureDate, timeSlot, items, "Aveiro");
        });
        
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testCancelWithValidToken() {
        Booking booking = createMockBooking(State.RECEIVED);
        String actualToken = booking.getToken();
        when(bookingRepository.findByToken(actualToken)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        boolean result = bookingService.cancel(actualToken);
        
        assertTrue(result);
        verify(bookingRepository).findByToken(actualToken);
        verify(bookingRepository).save(booking);
    }

    @Test
    void testCancelWithInvalidToken() {
        when(bookingRepository.findByToken("invalidToken")).thenReturn(Optional.empty());
        
        boolean result = bookingService.cancel("invalidToken");
        
        assertFalse(result);
        verify(bookingRepository).findByToken("invalidToken");
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testCancelAlreadyCancelledBooking() {
        Booking booking = createMockBooking(State.CANCELLED);
        String actualToken = booking.getToken();
        when(bookingRepository.findByToken(actualToken)).thenReturn(Optional.of(booking));
        
        boolean result = bookingService.cancel(actualToken);
        
        assertFalse(result);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testCancelFinishedBooking() {
        Booking booking = createMockBooking(State.FINISHED);
        String actualToken = booking.getToken();
        when(bookingRepository.findByToken(actualToken)).thenReturn(Optional.of(booking));
        
        boolean result = bookingService.cancel(actualToken);
        
        assertFalse(result);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testCheckWithValidToken() {
        Booking booking = createMockBooking(State.ASSIGNED);
        String actualToken = booking.getToken();
        when(bookingRepository.findByToken(actualToken)).thenReturn(Optional.of(booking));
        
        Booking result = bookingService.check(actualToken);
        
        assertNotNull(result);
        assertEquals(actualToken, result.getToken());
        verify(bookingRepository).findByToken(actualToken);
    }

    @Test
    void testCheckWithInvalidToken() {
        when(bookingRepository.findByToken("nonexistent")).thenReturn(Optional.empty());
        
        Booking result = bookingService.check("nonexistent");
        
        assertNull(result);
        verify(bookingRepository).findByToken("nonexistent");
    }

    @Test
    void testGetAllBookings() {
        List<Booking> bookings = Arrays.asList(
            createMockBooking(State.RECEIVED),
            createMockBooking(State.ASSIGNED),
            createMockBooking(State.IN_PROGRESS)
        );
        when(bookingRepository.findAll()).thenReturn(bookings);
        
        List<Booking> result = bookingService.getAllBookings();
        
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(bookingRepository).findAll();
    }

    @Test
    void testGetAllBookingsEmpty() {
        when(bookingRepository.findAll()).thenReturn(new ArrayList<>());
        
        List<Booking> result = bookingService.getAllBookings();
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(bookingRepository).findAll();
    }

    @Test
    void testChangeStateWithValidToken() {
        Booking booking = createMockBooking(State.ASSIGNED);
        String actualToken = booking.getToken();
        when(bookingRepository.findByToken(actualToken)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        boolean result = bookingService.changeState(actualToken, State.IN_PROGRESS);
        
        assertTrue(result);
        verify(bookingRepository).findByToken(actualToken);
        verify(bookingRepository).save(booking);
    }

    @Test
    void testChangeStateWithInvalidToken() {
        when(bookingRepository.findByToken("invalid")).thenReturn(Optional.empty());
        
        boolean result = bookingService.changeState("invalid", State.ASSIGNED);
        
        assertFalse(result);
        verify(bookingRepository).findByToken("invalid");
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testChangeStateWhenAlreadyFinished() {
        Booking booking = createMockBooking(State.FINISHED);
        String actualToken = booking.getToken();
        when(bookingRepository.findByToken(actualToken)).thenReturn(Optional.of(booking));
        
        boolean result = bookingService.changeState(actualToken, State.FINISHED);
        
        assertFalse(result);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testGetBookingsByState() {
        List<Booking> assignedBookings = Arrays.asList(
            createMockBooking(State.ASSIGNED),
            createMockBooking(State.ASSIGNED)
        );
        when(bookingRepository.findByCurrentState_State(State.ASSIGNED)).thenReturn(assignedBookings);
        
        List<Booking> result = bookingService.getBookingsByState(State.ASSIGNED);
        
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(bookingRepository).findByCurrentState_State(State.ASSIGNED);
    }

    @Test
    void testGetBookingsByMunicipality() {
        List<Booking> aveiroBookings = Arrays.asList(
            createMockBooking(State.RECEIVED),
            createMockBooking(State.ASSIGNED)
        );
        when(bookingRepository.findByMunicipality("Aveiro")).thenReturn(aveiroBookings);
        
        List<Booking> result = bookingService.getBookingsByMunicipality("Aveiro");
        
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(bookingRepository).findByMunicipality("Aveiro");
    }

    @Test
    void testBookingWithNullMunicipality() {
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.book(futureDate, timeSlot, items, null);
        });
        
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testBookingWithNullDate() {
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.book(null, timeSlot, items, "Aveiro");
        });
        
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testBookingWithNullTimeSlot() {
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.book(futureDate, null, items, "Aveiro");
        });
        
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testBookingWithWeekendDate() {
        LocalDate tempWeekend = LocalDate.now().plusDays(7);
        while (tempWeekend.getDayOfWeek().getValue() < 6) {
            tempWeekend = tempWeekend.plusDays(1);
        }
        final LocalDate weekend = tempWeekend;

        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.book(weekend, timeSlot, items, "Aveiro");
        });
        
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testBookingWithInvalidTimeSlot() {
        LocalTime invalidTime = LocalTime.of(22, 0);
        
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.book(futureDate, invalidTime, items, "Aveiro");
        });
        
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testMultipleBookingsOnSameDay() {
        when(municipalityProvider.isValid("Aveiro")).thenReturn(true);
        when(bookingRepository.findByDateAndApproxTimeSlotAndMunicipality(futureDate, timeSlot, "Aveiro"))
            .thenReturn(createManyBookings(5));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        String token = bookingService.book(futureDate, timeSlot, items, "Aveiro");
        
        assertNotNull(token);
        verify(bookingRepository).save(any(Booking.class));
    }

    private Booking createMockBooking(State state) {
        List<Item> items = new ArrayList<>();
        items.add(new Item("Test Item", "Description"));
        Booking booking = new Booking(
            LocalDate.now().plusDays(7),
            LocalTime.of(10, 0),
            items,
            "Aveiro"
        );
        
        while (booking.getCurrentState().getState() != state) {
            if (state == State.ASSIGNED && booking.getCurrentState().getState() == State.RECEIVED) {
                booking.changeState(State.ASSIGNED);
            } else if (state == State.IN_PROGRESS) {
                if (booking.getCurrentState().getState() == State.RECEIVED) {
                    booking.changeState(State.ASSIGNED);
                }
                booking.changeState(State.IN_PROGRESS);
            } else if (state == State.FINISHED) {
                if (booking.getCurrentState().getState() == State.RECEIVED) {
                    booking.changeState(State.ASSIGNED);
                }
                if (booking.getCurrentState().getState() == State.ASSIGNED) {
                    booking.changeState(State.IN_PROGRESS);
                }
                booking.changeState(State.FINISHED);
            } else if (state == State.CANCELLED) {
                booking.changeState(State.CANCELLED);
            } else if (state == State.REMOVED) {
                booking.changeState(State.REMOVED);
            }
        }
        
        return booking;
    }

    private List<Booking> createManyBookings(int count) {
        List<Booking> bookings = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            bookings.add(createMockBooking(State.ASSIGNED));
        }
        return bookings;
    }
}