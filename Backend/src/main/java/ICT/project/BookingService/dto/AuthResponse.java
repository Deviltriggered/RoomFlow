package ICT.project.BookingService.dto;

public record AuthResponse(
        Long userId,
        String email,
        String legalName,
        String phone,
        String role
) {
}
