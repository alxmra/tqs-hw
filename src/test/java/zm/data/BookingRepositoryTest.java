package zm.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class BookingRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookingRepository bookingRepository;

    private List<Item> sampleItems;

    @BeforeEach
    void setUp() {
        sampleItems = new ArrayList<>();
        sampleItems.add(new Item("Mattress", "Old mattress"));
        sampleItems.add(new Item("Sofa", "Leather sofa"));
    }

    @Test
    void testSaveBooking() {
        Booking booking = new Booking(
            LocalDate.now().plusDays(5),
            LocalTime.of(10, 0),
            sampleItems,
            "Aveiro"
        );

        Booking savedBooking = bookingRepository.save(booking);

        assertNotNull(savedBooking.getToken());
        assertEquals("Aveiro", savedBooking.getMunicipality());
        assertEquals(2, savedBooking.getItems().size());
    }

    @Test
    void testFindByToken() {
        Booking booking = new Booking(
            LocalDate.now().plusDays(5),
            LocalTime.of(10, 0),
            sampleItems,
            "Aveiro"
        );
        String token = booking.getToken();
        entityManager.persistAndFlush(booking);

        Optional<Booking> found = bookingRepository.findByToken(token);

        assertTrue(found.isPresent());
        assertEquals(token, found.get().getToken());
        assertEquals("Aveiro", found.get().getMunicipality());
    }

    @Test
    void testFindByTokenNotFound() {
        Optional<Booking> found = bookingRepository.findByToken("NONEXISTENT");

        assertFalse(found.isPresent());
    }

    @Test
    void testFindAll() {
        Booking booking1 = new Booking(LocalDate.now().plusDays(5), LocalTime.of(10, 0), sampleItems, "Aveiro");
        Booking booking2 = new Booking(LocalDate.now().plusDays(6), LocalTime.of(14, 0), sampleItems, "Lisboa");
        Booking booking3 = new Booking(LocalDate.now().plusDays(7), LocalTime.of(16, 0), sampleItems, "Porto");

        entityManager.persist(booking1);
        entityManager.persist(booking2);
        entityManager.persist(booking3);
        entityManager.flush();

        List<Booking> bookings = bookingRepository.findAll();

        assertTrue(bookings.size() >= 3);
    }

    @Test
    void testFindByState() {
        Booking booking1 = new Booking(LocalDate.now().plusDays(5), LocalTime.of(10, 0), sampleItems, "Aveiro");
        Booking booking2 = new Booking(LocalDate.now().plusDays(6), LocalTime.of(14, 0), sampleItems, "Lisboa");
        
        entityManager.persist(booking1);
        entityManager.persist(booking2);
        entityManager.flush();

        booking1.changeState(State.ASSIGNED);
        entityManager.persist(booking1);
        entityManager.flush();

        List<Booking> receivedBookings = bookingRepository.findByCurrentState_State(State.RECEIVED);
        List<Booking> assignedBookings = bookingRepository.findByCurrentState_State(State.ASSIGNED);

        assertTrue(receivedBookings.stream().anyMatch(b -> b.getToken().equals(booking2.getToken())));
        assertTrue(assignedBookings.stream().anyMatch(b -> b.getToken().equals(booking1.getToken())));
    }

    @Test
    void testFindByMunicipality() {
        Booking booking1 = new Booking(LocalDate.now().plusDays(5), LocalTime.of(10, 0), sampleItems, "Aveiro");
        Booking booking2 = new Booking(LocalDate.now().plusDays(6), LocalTime.of(14, 0), sampleItems, "Aveiro");
        Booking booking3 = new Booking(LocalDate.now().plusDays(7), LocalTime.of(16, 0), sampleItems, "Porto");

        entityManager.persist(booking1);
        entityManager.persist(booking2);
        entityManager.persist(booking3);
        entityManager.flush();

        List<Booking> aveiroBookings = bookingRepository.findByMunicipality("Aveiro");

        assertTrue(aveiroBookings.size() >= 2);
        assertTrue(aveiroBookings.stream().allMatch(b -> b.getMunicipality().equals("Aveiro")));
    }

    @Test
    void testFindByMunicipalityEmpty() {
        List<Booking> bookings = bookingRepository.findByMunicipality("NonexistentCity");

        assertNotNull(bookings);
        assertTrue(bookings.isEmpty());
    }

    @Test
    void testDeleteBooking() {
        Booking booking = new Booking(LocalDate.now().plusDays(5), LocalTime.of(10, 0), sampleItems, "Aveiro");
        entityManager.persistAndFlush(booking);
        String bookingToken = booking.getToken();

        bookingRepository.deleteById(bookingToken);
        entityManager.flush();

        Optional<Booking> found = bookingRepository.findById(bookingToken);
        assertFalse(found.isPresent());
    }

    @Test
    void testUpdateBooking() {
        Booking booking = new Booking(LocalDate.now().plusDays(5), LocalTime.of(10, 0), sampleItems, "Aveiro");
        entityManager.persistAndFlush(booking);

        booking.changeState(State.ASSIGNED);
        Booking updated = bookingRepository.save(booking);

        assertEquals(State.ASSIGNED, updated.getCurrentState().getState());
    }

    @Test
    void testBookingPersistenceWithItems() {
        Booking booking = new Booking(LocalDate.now().plusDays(5), LocalTime.of(10, 0), sampleItems, "Aveiro");
        entityManager.persistAndFlush(booking);

        Booking found = entityManager.find(Booking.class, booking.getToken());

        assertNotNull(found);
        assertEquals(2, found.getItems().size());
        assertTrue(found.getItems().stream().anyMatch(i -> i.getName().equals("Mattress")));
        assertTrue(found.getItems().stream().anyMatch(i -> i.getName().equals("Sofa")));
    }

    @Test
    void testBookingPersistenceWithStateHistory() {
        Booking booking = new Booking(LocalDate.now().plusDays(5), LocalTime.of(10, 0), sampleItems, "Aveiro");
        entityManager.persistAndFlush(booking);

        booking.changeState(State.ASSIGNED);
        booking.changeState(State.IN_PROGRESS);
        entityManager.persistAndFlush(booking);

        Booking found = entityManager.find(Booking.class, booking.getToken());

        assertNotNull(found);
        assertEquals(State.IN_PROGRESS, found.getCurrentState().getState());
        assertEquals(2, found.getPreviousStates().size());
    }

    @Test
    void testFindByStateEmpty() {
        List<Booking> bookings = bookingRepository.findByCurrentState_State(State.FINISHED);

        assertNotNull(bookings);
    }

    @Test
    void testCountByMunicipality() {
        Booking booking1 = new Booking(LocalDate.now().plusDays(5), LocalTime.of(10, 0), sampleItems, "Braga");
        Booking booking2 = new Booking(LocalDate.now().plusDays(6), LocalTime.of(14, 0), sampleItems, "Braga");

        entityManager.persist(booking1);
        entityManager.persist(booking2);
        entityManager.flush();

        List<Booking> bragaBookings = bookingRepository.findByMunicipality("Braga");

        assertTrue(bragaBookings.size() >= 2);
    }

    @Test
    void testTransactionalBehavior() {
        Booking booking = new Booking(LocalDate.now().plusDays(5), LocalTime.of(10, 0), sampleItems, "Aveiro");
        
        bookingRepository.save(booking);
        String token = booking.getToken();

        Optional<Booking> found = bookingRepository.findByToken(token);
        assertTrue(found.isPresent());
    }

    @Test
    void testCascadePersistence() {
        List<Item> items = new ArrayList<>();
        items.add(new Item("TV", "Old television"));
        items.add(new Item("Fridge", "Broken refrigerator"));
        items.add(new Item("Washing Machine", "Non-functional washer"));

        Booking booking = new Booking(LocalDate.now().plusDays(5), LocalTime.of(10, 0), items, "Coimbra");
        bookingRepository.save(booking);

        Optional<Booking> found = bookingRepository.findByToken(booking.getToken());

        assertTrue(found.isPresent());
        assertEquals(3, found.get().getItems().size());
    }
}