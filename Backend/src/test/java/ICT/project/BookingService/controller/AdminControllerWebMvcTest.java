package ICT.project.BookingService.controller;

import ICT.project.BookingService.BookingServiceApplication;
import ICT.project.BookingService.dto.AdminBookingResponse;
import ICT.project.BookingService.dto.PaymentResponse;
import ICT.project.BookingService.entity.UserEntity;
import ICT.project.BookingService.repository.BookingPaymentLinkRepository;
import ICT.project.BookingService.repository.BookingRepository;
import ICT.project.BookingService.repository.LocationRepository;
import ICT.project.BookingService.repository.PaymentRepository;
import ICT.project.BookingService.repository.TariffRepository;
import ICT.project.BookingService.repository.UserCredentialRepository;
import ICT.project.BookingService.repository.UserRepository;
import ICT.project.BookingService.security.JwtTokenService;
import ICT.project.BookingService.service.AdminService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = BookingServiceApplication.class,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
                "app.auth.jwt.secret=test-jwt-secret-booking-service-1234567890"
        }
)
@AutoConfigureMockMvc
class AdminControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private AdminService adminService;

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
    void getAllBookingsReturnsDataForAdminUser() throws Exception {
        PaymentResponse payment = new PaymentResponse(
                21L,
                new BigDecimal("2700.00"),
                "Unpaid",
                "Онлайн",
                LocalDateTime.of(2026, 4, 20, 9, 0),
                LocalDate.of(2026, 4, 21)
        );
        AdminBookingResponse booking = new AdminBookingResponse(
                11L,
                5L,
                "client@example.com",
                "ООО Ромашка",
                3L,
                "Зал «Орион»",
                "Деловой центр, корпус A",
                7L,
                "Базовый",
                LocalDateTime.of(2026, 4, 21, 10, 0),
                LocalDateTime.of(2026, 4, 21, 13, 0),
                3,
                new BigDecimal("2700.00"),
                "Unconfirmed",
                payment
        );
        when(adminService.getAllBookings()).thenReturn(List.of(booking));

        mockMvc.perform(get("/api/admin/bookings")
                        .header("Authorization", authorizationHeaderFor(5L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(11))
                .andExpect(jsonPath("$[0].userEmail").value("client@example.com"));
    }

    @Test
    void updateBookingStatusIsForbiddenForClientUser() throws Exception {
        mockMvc.perform(patch("/api/admin/bookings/11/status")
                        .header("Authorization", authorizationHeaderFor(6L, "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CONFIRMED"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private String authorizationHeaderFor(Long userId, String role) {
        UserEntity user = new UserEntity();
        user.setUserId(userId);
        user.setUserEmail("client@example.com");
        user.setUserLegalName("ООО Ромашка");
        user.setUserRole(role);
        return "Bearer " + jwtTokenService.issueSession(user).accessToken();
    }
}
