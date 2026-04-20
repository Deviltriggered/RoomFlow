package ICT.project.BookingService.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentSummaryResponse(
        Long paymentId,
        Long bookingId,
        String locationName,
        String tariffName,
        BigDecimal paymentSum,
        String paymentStatus,
        String paymentMethod,
        LocalDateTime paymentCreatedAt,
        LocalDate paymentDueDate
) {
}
