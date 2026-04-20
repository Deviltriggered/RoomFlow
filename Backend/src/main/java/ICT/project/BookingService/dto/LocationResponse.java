package ICT.project.BookingService.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record LocationResponse(
        Long locationId,
        String type,
        String name,
        String address,
        LocalTime opening,
        LocalTime closing,
        String phone,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
