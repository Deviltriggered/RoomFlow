package ICT.project.BookingService.repository;

import ICT.project.BookingService.entity.PaymentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    List<PaymentEntity> findAllByUserUserIdOrderByPaymentCreatedAtDesc(Long userId);
}
