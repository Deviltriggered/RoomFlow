package ICT.project.BookingService.service;

import ICT.project.BookingService.dto.AuthResponse;
import ICT.project.BookingService.dto.LoginRequest;
import ICT.project.BookingService.dto.RegisterRequest;
import ICT.project.BookingService.entity.UserCredentialEntity;
import ICT.project.BookingService.entity.UserEntity;
import ICT.project.BookingService.repository.UserCredentialRepository;
import ICT.project.BookingService.repository.UserRepository;
import ICT.project.BookingService.support.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static ICT.project.BookingService.security.SessionAuthenticationFilter.AUTH_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerNormalizesEmailStoresCredentialAndSession() {
        RegisterRequest request = new RegisterRequest("  TEST@Example.COM ", "ООО Ромашка", "   ", "secret1");
        MockHttpSession session = new MockHttpSession();

        when(userRepository.existsByUserEmailIgnoreCase(request.email())).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("encoded-secret");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setUserId(15L);
            return user;
        });

        AuthResponse response = authService.register(request, session);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();
        assertEquals("test@example.com", savedUser.getUserEmail());
        assertEquals("ООО Ромашка", savedUser.getUserLegalName());
        assertNull(savedUser.getUserPhone());
        assertEquals("CLIENT", savedUser.getUserRole());

        ArgumentCaptor<UserCredentialEntity> credentialCaptor = ArgumentCaptor.forClass(UserCredentialEntity.class);
        verify(userCredentialRepository).save(credentialCaptor.capture());
        assertEquals("encoded-secret", credentialCaptor.getValue().getPasswordHash());

        assertEquals(15L, session.getAttribute(AUTH_USER_ID));
        assertEquals(15L, response.userId());
        assertEquals("test@example.com", response.email());
    }

    @Test
    void loginThrowsUnauthorizedWhenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");
        MockHttpSession session = new MockHttpSession();

        UserEntity user = new UserEntity();
        user.setUserId(9L);
        user.setUserEmail("user@example.com");

        UserCredentialEntity credential = new UserCredentialEntity();
        credential.setPasswordHash("stored-hash");

        when(userRepository.findByUserEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(userCredentialRepository.findById(9L)).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("wrong-password", "stored-hash")).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class, () -> authService.login(request, session));

        assertEquals(UNAUTHORIZED, exception.getStatus());
        assertTrue(exception.getMessage().contains("Неверный email или пароль"));
    }
}
