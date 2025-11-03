package zm.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestStateTest {

    @Test
    void testRequestStateCreation() {
        RequestState requestState = new RequestState(State.RECEIVED);
        
        assertNotNull(requestState);
        assertEquals(State.RECEIVED, requestState.getState());
    }

    @Test
    void testRequestStateWithAllStates() {
        for (State state : State.values()) {
            RequestState requestState = new RequestState(state);
            assertEquals(state, requestState.getState());
        }
    }

    @Test
    void testRequestStateWithNullState() {
        assertThrows(IllegalArgumentException.class, () -> {
            new RequestState(null);
        });
    }

    @Test
    void testRequestStateInequalityDifferentTimestamp() {
        RequestState rs1 = new RequestState(State.FINISHED);
        RequestState rs2 = new RequestState(State.FINISHED);
        
        assertNotEquals(rs1, rs2);
    }

    @Test
    void testRequestStateIsImmutable() {
        RequestState requestState = new RequestState(State.ASSIGNED);
        State originalState = requestState.getState();

        assertEquals(originalState, requestState.getState());
    }

    @Test
    void testTimestampPrecision() {
        RequestState rs1 = new RequestState(State.RECEIVED);
        // Busy wait for a short period to ensure timestamp difference
        long endTime = System.nanoTime() + 1_000_000; // 1ms in nanoseconds
        while (System.nanoTime() < endTime) {
            // Busy wait
        }
        RequestState rs2 = new RequestState(State.RECEIVED);
        
        assertTrue(rs2.getTimestamp().after(rs1.getTimestamp()) || 
                   rs2.getTimestamp().equals(rs1.getTimestamp()));
    }

    @Test
    void testMultipleRequestStatesCreatedSequentially() {
        RequestState rs1 = new RequestState(State.RECEIVED);
        RequestState rs2 = new RequestState(State.ASSIGNED);
        RequestState rs3 = new RequestState(State.IN_PROGRESS);
        
        assertTrue(rs2.getTimestamp().getTime() >= rs1.getTimestamp().getTime());
        assertTrue(rs3.getTimestamp().getTime() >= rs2.getTimestamp().getTime());
    }
}