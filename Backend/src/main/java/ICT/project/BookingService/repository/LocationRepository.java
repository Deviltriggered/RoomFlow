package ICT.project.BookingService.repository;

import ICT.project.BookingService.entity.LocationEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<LocationEntity, Long> {

    List<LocationEntity> findAllByOrderByLocationNameAsc();
}
