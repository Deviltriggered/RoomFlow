package ICT.project.BookingService.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CreateBookingRequest(
        @NotNull(message = "Помещение обязательно")
        Long locationId,
        @NotNull(message = "Тариф обязателен")
        Long tariffId,
        @NotNull(message = "Укажите время начала")
        LocalDateTime bookingStart,
        @NotNull(message = "Укажите время окончания")
        LocalDateTime bookingEnd
) {
}
