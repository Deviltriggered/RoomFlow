package ICT.project.BookingService.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateBookingStatusRequest(
        @NotBlank(message = "Статус бронирования обязателен")
        String status
) {
}
