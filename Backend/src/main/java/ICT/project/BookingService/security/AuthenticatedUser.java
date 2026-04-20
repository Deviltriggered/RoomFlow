package ICT.project.BookingService.security;

public record AuthenticatedUser(
        Long userId,
        String email,
        String legalName,
        String phone,
        String role
) {
}
