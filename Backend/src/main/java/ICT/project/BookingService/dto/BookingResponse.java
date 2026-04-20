package ICT.project.BookingService.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long bookingId,
        Long locationId,
        String locationName,
        String locationAddress,
        Long tariffId,
        String tariffName,
        LocalDateTime bookingStart,
        LocalDateTime bookingEnd,
        Integer bookingDurationHours,
        BigDecimal bookingPrice,
        String bookingStatus,
        PaymentResponse payment
) {
}
