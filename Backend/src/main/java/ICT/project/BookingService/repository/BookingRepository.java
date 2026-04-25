package ICT.project.BookingService.repository;

import ICT.project.BookingService.entity.BookingEntity;
import ICT.project.BookingService.entity.BookingStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

    List<BookingEntity> findAllByOrderByBookingStartDesc();

    @Query("""
            select case when count(b) > 0 then true else false end
            from BookingEntity b
            where b.location.locationId = :locationId
              and b.bookingStatus <> :finishedStatus
              and :bookingStart < b.bookingEnd
              and :bookingEnd > b.bookingStart
            """)
    boolean existsConflictingBooking(
            @Param("locationId") Long locationId,
            @Param("finishedStatus") BookingStatus finishedStatus,
            @Param("bookingStart") LocalDateTime bookingStart,
            @Param("bookingEnd") LocalDateTime bookingEnd
    );

    List<BookingEntity> findAllByUserUserIdOrderByBookingStartDesc(Long userId);

    @Query("""
            select b
            from BookingEntity b
            where b.location.locationId = :locationId
              and b.bookingStatus <> :finishedStatus
              and b.bookingStart < :dayEnd
              and b.bookingEnd > :dayStart
            order by b.bookingStart asc
            """)
    List<BookingEntity> findAllActiveByLocationAndDate(
            @Param("locationId") Long locationId,
            @Param("finishedStatus") BookingStatus finishedStatus,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd
    );
}
