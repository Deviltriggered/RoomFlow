package ICT.project.BookingService.service;

import ICT.project.BookingService.dto.AdminBookingResponse;
import ICT.project.BookingService.dto.UpdateBookingStatusRequest;
import ICT.project.BookingService.entity.BookingEntity;
import ICT.project.BookingService.entity.BookingPaymentLinkEntity;
import ICT.project.BookingService.entity.BookingStatus;
import ICT.project.BookingService.entity.LocationEntity;
import ICT.project.BookingService.entity.PaymentEntity;
import ICT.project.BookingService.entity.PaymentStatus;
import ICT.project.BookingService.entity.TariffEntity;
import ICT.project.BookingService.entity.UserEntity;
import ICT.project.BookingService.repository.BookingPaymentLinkRepository;
import ICT.project.BookingService.repository.BookingRepository;
import ICT.project.BookingService.repository.LocationRepository;
import ICT.project.BookingService.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingPaymentLinkRepository bookingPaymentLinkRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void updateBookingStatusReplacesStatusAndReturnsUpdatedBooking() {
        BookingEntity booking = buildBooking();
        PaymentEntity payment = buildPayment();
        BookingPaymentLinkEntity link = new BookingPaymentLinkEntity();
        link.setBookingId(booking.getBookingId());
        link.setPaymentId(payment.getPaymentId());

        when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingPaymentLinkRepository.findById(booking.getBookingId())).thenReturn(Optional.of(link));
        when(paymentRepository.findById(payment.getPaymentId())).thenReturn(Optional.of(payment));

        AdminBookingResponse response = adminService.updateBookingStatus(
                booking.getBookingId(),
                new UpdateBookingStatusRequest("CONFIRMED")
        );

        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
        assertEquals("Confirmed", response.bookingStatus());
        assertEquals("client@example.com", response.userEmail());
    }

    @Test
    void deleteBookingRemovesLinkedPaymentAndBooking() {
        BookingEntity booking = buildBooking();
        BookingPaymentLinkEntity link = new BookingPaymentLinkEntity();
        link.setBookingId(booking.getBookingId());
        link.setPaymentId(55L);

        when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));
        when(bookingPaymentLinkRepository.findById(booking.getBookingId())).thenReturn(Optional.of(link));

        adminService.deleteBooking(booking.getBookingId());

        verify(paymentRepository).deleteById(55L);
        verify(bookingRepository).delete(booking);
        verify(locationRepository, never()).save(any());
    }

    private BookingEntity buildBooking() {
        UserEntity user = new UserEntity();
        user.setUserId(7L);
        user.setUserEmail("client@example.com");
        user.setUserLegalName("ООО Клиент");

        LocationEntity location = new LocationEntity();
        location.setLocationId(3L);
        location.setLocationName("Зал «Волга»");
        location.setLocationAddress("Деловой центр, корпус A");

        TariffEntity tariff = new TariffEntity();
        tariff.setTariffId(4L);
        tariff.setTariffName("Базовый");

        BookingEntity booking = new BookingEntity();
        booking.setBookingId(12L);
        booking.setUser(user);
        booking.setLocation(location);
        booking.setTariff(tariff);
        booking.setBookingStart(LocalDateTime.of(2026, 4, 21, 10, 0));
        booking.setBookingEnd(LocalDateTime.of(2026, 4, 21, 12, 0));
        booking.setBookingDuration(2);
        booking.setBookingPrice(new BigDecimal("2400.00"));
        booking.setBookingStatus(BookingStatus.UNCONFIRMED);
        return booking;
    }

    private PaymentEntity buildPayment() {
        TariffEntity tariff = new TariffEntity();
        tariff.setTariffId(4L);
        tariff.setTariffName("Базовый");

        UserEntity user = new UserEntity();
        user.setUserId(7L);

        PaymentEntity payment = new PaymentEntity();
        payment.setPaymentId(55L);
        payment.setUser(user);
        payment.setTariff(tariff);
        payment.setPaymentSum(new BigDecimal("2400.00"));
        payment.setPaymentStatus(PaymentStatus.UNPAID);
        payment.setPaymentMethod("Онлайн");
        payment.setPaymentCreatedAt(LocalDateTime.of(2026, 4, 20, 9, 0));
        payment.setPaymentDueDate(LocalDate.of(2026, 4, 21));
        return payment;
    }
}
