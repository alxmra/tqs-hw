package zm.data;

import jakarta.persistence.*;
import lombok.Setter;

import java.util.UUID;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Setter
    @Column(nullable = false)
    private LocalDate date;

    @Setter
    @Column(nullable = false)
    private LocalTime approxTimeSlot;

    @Setter
    @ElementCollection
    @CollectionTable(name = "booking_items", joinColumns = @JoinColumn(name = "booking_token"))
    private List<Item> items;

    @Setter
    @Column(nullable = false)
    private String municipality;

    @Id
    @Column(unique = true, nullable = false)
    private String token = UUID.randomUUID().toString();

    @Column(nullable = false)
    @Embedded
    private RequestState currentState = new RequestState(State.RECEIVED);

    @Column(nullable = false)
    @ElementCollection
    @CollectionTable(name = "booking_previous_states", joinColumns = @JoinColumn(name = "booking_token"))
    private List<RequestState> previousStates = new java.util.ArrayList<>();

    public Booking() {
    }

    public Booking(LocalDate date, LocalTime approxTimeSlot, List<Item> items, String municipality) {
        if (date != null && date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Date must be in the future");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Items list cannot be empty");
        }
        if (municipality == null || municipality.trim().isEmpty()) {
            throw new IllegalArgumentException("Municipality cannot be null or empty");
        }
        this.date = date;
        this.approxTimeSlot = approxTimeSlot;
        this.items = items;
        this.municipality = municipality;
    }

    public String getToken() {
        return token;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getApproxTimeSlot() {
        return approxTimeSlot;
    }

    public List<Item> getItems() {
        return items;
    }

    public String getMunicipality() {
        return municipality;
    }

    public RequestState getCurrentState() {
        return currentState;
    }

    public List<RequestState> getPreviousStates() {
        return previousStates;
    }

    public boolean changeState(State newState) {
        if (currentState.getState() == newState) {
            return false;
        }

        State current = currentState.getState();

        if (current == State.FINISHED || current == State.CANCELLED || current == State.REMOVED) {
            return false;
        }

        if (newState == State.RECEIVED && current != State.RECEIVED) {
            return false;
        }

        previousStates.add(currentState);
        currentState = new RequestState(newState);
        return true;
    }
}
