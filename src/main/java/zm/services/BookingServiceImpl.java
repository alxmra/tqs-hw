package zm.services;

import org.springframework.stereotype.Service;
import zm.data.Booking;
import zm.data.BookingRepository;
import zm.data.Item;
import zm.data.State;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository collectionRepo;
    private final List<String> municipalityBlacklist = new ArrayList<>();
    private final MunicipalityProvider municipalityProvider;

    public BookingServiceImpl(BookingRepository collectionRepo, MunicipalityProvider municipalityProvider) {
        this.collectionRepo = collectionRepo;
        this.municipalityProvider = municipalityProvider;
    }

    public String book(LocalDate date, LocalTime time, List<Item> items, String municipality) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Date must be in the future");
        }
        if (time == null) {
            throw new IllegalArgumentException("Time slot cannot be null");
        }
        if (municipality == null) {
            throw new IllegalArgumentException("Municipality cannot be null");
        }
        if (date.getDayOfWeek().getValue() >= 6) {
            throw new IllegalArgumentException("Bookings cannot be made on weekends");
        }
        if (time.getHour() < 8 || time.getHour() >= 18) {
            throw new IllegalArgumentException("Invalid time slot - must be between 8:00 and 18:00");
        }
        if (items != null && items.size() > 10) {
            throw new IllegalArgumentException("Too many items");
        }
        if (municipalityBlacklist.contains(municipality)) {
            throw new IllegalArgumentException("Municipality is blacklisted");
        }
        if (!this.municipalityProvider.isValid(municipality)) {
            throw new IllegalArgumentException("Invalid municipality");
        }

        List<Booking> existingBookings = collectionRepo.findByDateAndApproxTimeSlotAndMunicipality(date, time, municipality);
        if (existingBookings.size() >= 50) {
            throw new IllegalStateException("Capacity exceeded");
        }

        Booking b = new Booking(date, time, items, municipality);
        collectionRepo.save(b);
        return b.getToken();
    }

    public boolean cancel(String token) {
        Optional<Booking> optb = collectionRepo.findByToken(token);
        if (optb.isPresent()) {
            Booking b = optb.get();
            if (b.getCurrentState().getState() == State.CANCELLED)
                return false;
            if (b.changeState(State.CANCELLED)) {
                collectionRepo.save(b);
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean remove(String token) {
        Optional<Booking> optb = collectionRepo.findByToken(token);
        if (optb.isPresent()) {
            Booking b = optb.get();
            if (b.getCurrentState().getState() == State.REMOVED)
                return false;
            if (b.changeState(State.REMOVED)) {
                collectionRepo.save(b);
                return true;
            }
            return false;
        }
        return false;
    }

    public Booking check(String token) {
        Optional<Booking> optb = collectionRepo.findByToken(token);
        return optb.orElse(null);
    }

    public List<Booking> getAllBookings() {
        return collectionRepo.findAll();
    }

    public boolean changeState(String token, State newstate) {
        Optional<Booking> optb = collectionRepo.findByToken(token);
        if (optb.isPresent()) {
            Booking b = optb.get();
            if (b.changeState(newstate)) {
                collectionRepo.save(b);
                return true;
            }
            return false;
        }
        return false;
    }

    public List<Booking> getBookingsByState(State state) {
        return collectionRepo.findByCurrentState_State(state);
    }

    public List<Booking> getBookingsByMunicipality(String municipality) {
        return collectionRepo.findByMunicipality(municipality);
    }
}
