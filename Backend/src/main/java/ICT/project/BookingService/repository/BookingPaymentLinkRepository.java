package ICT.project.BookingService.repository;

import ICT.project.BookingService.entity.BookingPaymentLinkEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingPaymentLinkRepository extends JpaRepository<BookingPaymentLinkEntity, Long> {

    @Query("select link from BookingPaymentLinkEntity link where link.bookingId in :bookingIds")
    List<BookingPaymentLinkEntity> findAllByBookingIds(@Param("bookingIds") Collection<Long> bookingIds);

    @Query("select link from BookingPaymentLinkEntity link where link.paymentId in :paymentIds")
    List<BookingPaymentLinkEntity> findAllByPaymentIds(@Param("paymentIds") Collection<Long> paymentIds);
}
