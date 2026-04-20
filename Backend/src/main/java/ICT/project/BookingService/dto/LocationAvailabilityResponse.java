package ICT.project.BookingService.dto;

import java.time.LocalDate;
import java.util.List;

public record LocationAvailabilityResponse(
        Long locationId,
        LocalDate date,
        List<Integer> occupiedHours
) {
}
