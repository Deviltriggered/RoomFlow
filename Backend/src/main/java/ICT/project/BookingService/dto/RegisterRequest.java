package ICT.project.BookingService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email(message = "Укажите корректный email")
        @NotBlank(message = "Email обязателен")
        String email,
        @NotBlank(message = "Имя обязательно")
        @Size(min = 2, max = 255, message = "Имя должно содержать от 2 до 255 символов")
        String legalName,
        @Size(max = 30, message = "Телефон не должен превышать 30 символов")
        String phone,
        @NotBlank(message = "Пароль обязателен")
        @Size(min = 6, max = 100, message = "Пароль должен содержать от 6 до 100 символов")
        String password
) {
}
