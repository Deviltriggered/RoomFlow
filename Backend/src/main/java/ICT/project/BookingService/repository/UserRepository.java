package ICT.project.BookingService.repository;

import ICT.project.BookingService.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByUserEmailIgnoreCase(String email);

    Optional<UserEntity> findByUserEmailIgnoreCase(String email);
}
