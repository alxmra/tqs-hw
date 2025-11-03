package zm.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {
    public Optional<Booking> findByToken(String token);
    public List<Booking> findByCurrentState_State(State currentState);
    public List<Booking> findByMunicipality(String municipality);
    public List<Booking> findByDateAndApproxTimeSlotAndMunicipality(LocalDate date, LocalTime time, String municipality);

}
