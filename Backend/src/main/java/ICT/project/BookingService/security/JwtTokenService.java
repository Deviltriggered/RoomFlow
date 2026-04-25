package ICT.project.BookingService.security;

import ICT.project.BookingService.dto.AuthResponse;
import ICT.project.BookingService.dto.AuthSessionResponse;
import ICT.project.BookingService.entity.UserEntity;
import ICT.project.BookingService.support.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final ObjectMapper objectMapper;
    private final byte[] secretKey;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final Clock clock;

    public JwtTokenService(
            @Value("${app.auth.jwt.secret}") String secret,
            @Value("${app.auth.jwt.access-token-ttl:30m}") Duration accessTokenTtl,
            @Value("${app.auth.jwt.refresh-token-ttl:14d}") Duration refreshTokenTtl
    ) {
        if (secret == null || secret.strip().length() < 32) {
            throw new IllegalStateException("JWT secret must contain at least 32 characters.");
        }
        this.objectMapper = new ObjectMapper();
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
        this.clock = Clock.systemUTC();
    }

    public AuthSessionResponse issueSession(UserEntity user) {
        return issueSession(toPrincipal(user));
    }

    public AuthenticatedUser parseAccessToken(String token) {
        return parseToken(token, ACCESS_TOKEN_TYPE);
    }

    public AuthenticatedUser parseRefreshToken(String token) {
        return parseToken(token, REFRESH_TOKEN_TYPE);
    }

    public AuthenticatedUser toPrincipal(UserEntity user) {
        return new AuthenticatedUser(
                user.getUserId(),
                user.getUserEmail(),
                user.getUserLegalName(),
                user.getUserPhone(),
                user.getUserRole()
        );
    }

    private AuthSessionResponse issueSession(AuthenticatedUser user) {
        Instant issuedAt = Instant.now(clock);
        Instant accessTokenExpiresAt = issuedAt.plus(accessTokenTtl);
        Instant refreshTokenExpiresAt = issuedAt.plus(refreshTokenTtl);
        return new AuthSessionResponse(
                new AuthResponse(user.userId(), user.email(), user.legalName(), user.phone(), user.role()),
                createToken(user, ACCESS_TOKEN_TYPE, issuedAt, accessTokenExpiresAt),
                accessTokenExpiresAt,
                createToken(user, REFRESH_TOKEN_TYPE, issuedAt, refreshTokenExpiresAt),
                refreshTokenExpiresAt
        );
    }

    private AuthenticatedUser parseToken(String token, String expectedType) {
        Map<String, Object> claims = decodeAndVerify(token);
        String tokenType = asString(claims.get("token_type"));
        if (!expectedType.equals(tokenType)) {
            throw unauthorized("Некорректный тип токена.");
        }

        return new AuthenticatedUser(
                readLongClaim(claims, "uid"),
                asString(claims.get("email")),
                asString(claims.get("legal_name")),
                asNullableString(claims.get("phone")),
                asString(claims.get("role"))
        );
    }

    private Map<String, Object> decodeAndVerify(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw unauthorized("Некорректный токен.");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        byte[] expectedSignature = sign(unsignedToken.getBytes(StandardCharsets.US_ASCII));
        byte[] actualSignature;
        byte[] payloadBytes;
        try {
            actualSignature = URL_DECODER.decode(parts[2]);
            payloadBytes = URL_DECODER.decode(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw unauthorized("Некорректный токен.");
        }

        if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
            throw unauthorized("Некорректная подпись токена.");
        }

        Map<String, Object> claims;
        try {
            claims = objectMapper.readValue(payloadBytes, MAP_TYPE);
        } catch (Exception exception) {
            throw unauthorized("Некорректное содержимое токена.");
        }

        long expiresAtEpochSecond = readLongClaim(claims, "exp");
        if (Instant.ofEpochSecond(expiresAtEpochSecond).isBefore(Instant.now(clock))
                || Instant.ofEpochSecond(expiresAtEpochSecond).equals(Instant.now(clock))) {
            throw unauthorized("Срок действия токена истёк.");
        }

        return claims;
    }

    private String createToken(AuthenticatedUser user, String tokenType, Instant issuedAt, Instant expiresAt) {
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", String.valueOf(user.userId()));
        claims.put("uid", user.userId());
        claims.put("email", user.email());
        claims.put("legal_name", user.legalName());
        claims.put("phone", user.phone());
        claims.put("role", user.role());
        claims.put("token_type", tokenType);
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());

        try {
            String encodedHeader = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(header));
            String encodedClaims = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(claims));
            String unsignedToken = encodedHeader + "." + encodedClaims;
            String encodedSignature = URL_ENCODER.encodeToString(sign(unsignedToken.getBytes(StandardCharsets.US_ASCII)));
            return unsignedToken + "." + encodedSignature;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create JWT token.", exception);
        }
    }

    private byte[] sign(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to sign JWT token.", exception);
        }
    }

    private long readLongClaim(Map<String, Object> claims, String claimName) {
        Object value = claims.get(claimName);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException exception) {
                throw unauthorized("Некорректное поле токена: " + claimName + '.');
            }
        }
        throw unauthorized("Некорректное поле токена: " + claimName + '.');
    }

    private String asString(Object value) {
        String text = asNullableString(value);
        if (text == null || text.isBlank()) {
            throw unauthorized("В токене отсутствуют обязательные данные.");
        }
        return text;
    }

    private String asNullableString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }
}
