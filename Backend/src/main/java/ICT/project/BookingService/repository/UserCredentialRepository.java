package ICT.project.BookingService.repository;

import ICT.project.BookingService.entity.UserCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCredentialRepository extends JpaRepository<UserCredentialEntity, Long> {
}
