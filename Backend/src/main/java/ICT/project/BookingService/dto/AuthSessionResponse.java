package ICT.project.BookingService.dto;

import java.time.Instant;

public record AuthSessionResponse(
        AuthResponse user,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}
