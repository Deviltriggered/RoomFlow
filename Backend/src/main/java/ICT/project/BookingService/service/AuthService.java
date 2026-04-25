package ICT.project.BookingService.service;

import ICT.project.BookingService.dto.AuthSessionResponse;
import ICT.project.BookingService.dto.LoginRequest;
import ICT.project.BookingService.dto.RegisterRequest;
import ICT.project.BookingService.entity.UserCredentialEntity;
import ICT.project.BookingService.entity.UserEntity;
import ICT.project.BookingService.repository.UserCredentialRepository;
import ICT.project.BookingService.repository.UserRepository;
import ICT.project.BookingService.security.AuthenticatedUser;
import ICT.project.BookingService.security.JwtTokenService;
import ICT.project.BookingService.support.ApiException;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final String adminEmail;

    public AuthService(
            UserRepository userRepository,
            UserCredentialRepository userCredentialRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            @Value("${app.auth.admin-email:}") String adminEmail
    ) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.adminEmail = normalizeOptionalEmail(adminEmail);
    }

    public AuthSessionResponse register(RegisterRequest request) {
        if (userRepository.existsByUserEmailIgnoreCase(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Пользователь с таким email уже существует.");
        }

        UserEntity user = new UserEntity();
        user.setUserEmail(normalizeEmail(request.email()));
        user.setUserLegalName(request.legalName().trim());
        user.setUserPhone(blankToNull(request.phone()));
        user.setUserRole(determineRoleForEmail(request.email()));
        user = userRepository.save(user);

        UserCredentialEntity credential = new UserCredentialEntity();
        credential.setUser(user);
        credential.setPasswordHash(passwordEncoder.encode(request.password()));
        credential.setCreatedAt(LocalDateTime.now());
        userCredentialRepository.save(credential);

        return jwtTokenService.issueSession(user);
    }

    public AuthSessionResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUserEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Неверный email или пароль."));

        UserCredentialEntity credential = userCredentialRepository.findById(user.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Неверный email или пароль."));

        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Неверный email или пароль.");
        }

        return jwtTokenService.issueSession(user);
    }

    public AuthSessionResponse refresh(String refreshToken) {
        AuthenticatedUser principal = jwtTokenService.parseRefreshToken(refreshToken);
        UserEntity user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Пользователь не найден."));
        return jwtTokenService.issueSession(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String determineRoleForEmail(String email) {
        return !adminEmail.isBlank() && adminEmail.equals(normalizeEmail(email)) ? "ADMIN" : "CLIENT";
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeOptionalEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
