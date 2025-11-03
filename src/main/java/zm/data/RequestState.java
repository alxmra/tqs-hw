package zm.data;

import jakarta.persistence.Embeddable;

import java.sql.Timestamp;
import java.time.Instant;

@Embeddable
public class RequestState {
    private State state;
    private Timestamp timestamp;

    public RequestState() {
        this.state = State.RECEIVED;
        this.timestamp = Timestamp.from(Instant.now());
    }

    public RequestState(State state) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }
        this.state = state;
        this.timestamp = Timestamp.from(Instant.now());
    }

    public State getState() {
        return state;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
}
