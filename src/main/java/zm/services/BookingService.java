package zm.services;

import org.springframework.stereotype.Service;
import zm.data.Booking;
import zm.data.Item;
import zm.data.State;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public interface BookingService {
    public String book(LocalDate date, LocalTime time, List<Item> items, String municipality);
    public boolean cancel(String token);
    public Booking check(String token);
    public List<Booking> getAllBookings();
    public boolean changeState(String token, State newstate);
    public List<Booking> getBookingsByState(State state);
    public List<Booking> getBookingsByMunicipality(String municipality);
}
