package ICT.project.BookingService.repository;

import ICT.project.BookingService.entity.TariffEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TariffRepository extends JpaRepository<TariffEntity, Long> {

    List<TariffEntity> findAllByOrderByTariffBasePriceAsc();
}
