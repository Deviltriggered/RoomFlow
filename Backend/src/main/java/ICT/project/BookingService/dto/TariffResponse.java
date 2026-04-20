package ICT.project.BookingService.dto;

import java.math.BigDecimal;

public record TariffResponse(
        Long tariffId,
        String name,
        String type,
        BigDecimal basePrice,
        BigDecimal discount,
        String status
) {
}
