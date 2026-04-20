package ICT.project.BookingService.service;

import ICT.project.BookingService.dto.AuthResponse;
import ICT.project.BookingService.dto.LoginRequest;
import ICT.project.BookingService.dto.RegisterRequest;
import ICT.project.BookingService.entity.UserCredentialEntity;
import ICT.project.BookingService.entity.UserEntity;
import ICT.project.BookingService.repository.UserCredentialRepository;
import ICT.project.BookingService.repository.UserRepository;
import ICT.project.BookingService.support.ApiException;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static ICT.project.BookingService.security.SessionAuthenticationFilter.AUTH_USER_ID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            UserCredentialRepository userCredentialRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request, HttpSession session) {
        if (userRepository.existsByUserEmailIgnoreCase(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Пользователь с таким email уже существует.");
        }

        UserEntity user = new UserEntity();
        user.setUserEmail(normalizeEmail(request.email()));
        user.setUserLegalName(request.legalName().trim());
        user.setUserPhone(blankToNull(request.phone()));
        user.setUserRole("CLIENT");
        user = userRepository.save(user);

        UserCredentialEntity credential = new UserCredentialEntity();
        credential.setUser(user);
        credential.setPasswordHash(passwordEncoder.encode(request.password()));
        credential.setCreatedAt(LocalDateTime.now());
        userCredentialRepository.save(credential);

        session.setAttribute(AUTH_USER_ID, user.getUserId());
        return toResponse(user);
    }

    public AuthResponse login(LoginRequest request, HttpSession session) {
        UserEntity user = userRepository.findByUserEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Неверный email или пароль."));

        UserCredentialEntity credential = userCredentialRepository.findById(user.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Неверный email или пароль."));

        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Неверный email или пароль.");
        }

        session.setAttribute(AUTH_USER_ID, user.getUserId());
        return toResponse(user);
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    public AuthResponse toResponse(UserEntity user) {
        return new AuthResponse(
                user.getUserId(),
                user.getUserEmail(),
                user.getUserLegalName(),
                user.getUserPhone(),
                user.getUserRole()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
