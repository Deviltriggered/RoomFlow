package ICT.project.BookingService.controller;

import ICT.project.BookingService.dto.AuthResponse;
import ICT.project.BookingService.BookingServiceApplication;
import ICT.project.BookingService.repository.BookingPaymentLinkRepository;
import ICT.project.BookingService.repository.BookingRepository;
import ICT.project.BookingService.repository.LocationRepository;
import ICT.project.BookingService.repository.PaymentRepository;
import ICT.project.BookingService.repository.TariffRepository;
import ICT.project.BookingService.repository.UserCredentialRepository;
import ICT.project.BookingService.repository.UserRepository;
import ICT.project.BookingService.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = BookingServiceApplication.class,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
        }
)
@AutoConfigureMockMvc
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserCredentialRepository userCredentialRepository;

    @MockitoBean
    private BookingRepository bookingRepository;

    @MockitoBean
    private BookingPaymentLinkRepository bookingPaymentLinkRepository;

    @MockitoBean
    private LocationRepository locationRepository;

    @MockitoBean
    private PaymentRepository paymentRepository;

    @MockitoBean
    private TariffRepository tariffRepository;

    @Test
    void registerReturnsCreatedProfile() throws Exception {
        AuthResponse response = new AuthResponse(4L, "client@example.com", "ООО Ромашка", null, "CLIENT");
        when(authService.register(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "client@example.com",
                                  "legalName": "ООО Ромашка",
                                  "phone": "",
                                  "password": "secret1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(4))
                .andExpect(jsonPath("$.email").value("client@example.com"))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    void registerReturnsValidationMessageForInvalidBody() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "wrong-email",
                                  "legalName": "",
                                  "phone": "123",
                                  "password": "123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("email")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Пароль должен содержать")));
    }

    @Test
    void meReturnsUnauthorizedWithoutAuthenticatedSession() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
