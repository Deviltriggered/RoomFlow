package ICT.project.BookingService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalTime;

public record UpsertLocationRequest(
        @NotBlank(message = "Тип помещения обязателен")
        String type,
        @NotBlank(message = "Название помещения обязательно")
        String name,
        @NotBlank(message = "Адрес помещения обязателен")
        String address,
        @NotNull(message = "Время открытия обязательно")
        LocalTime opening,
        @NotNull(message = "Время закрытия обязательно")
        LocalTime closing,
        String phone,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
