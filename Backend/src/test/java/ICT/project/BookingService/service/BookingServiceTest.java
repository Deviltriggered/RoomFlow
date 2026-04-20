package ICT.project.BookingService.service;

import ICT.project.BookingService.dto.BookingResponse;
import ICT.project.BookingService.dto.CreateBookingRequest;
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
import ICT.project.BookingService.repository.TariffRepository;
import ICT.project.BookingService.repository.UserRepository;
import ICT.project.BookingService.support.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingPaymentLinkRepository bookingPaymentLinkRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private TariffRepository tariffRepository;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void createBookingCreatesBookingPaymentAndLink() {
        LocalDateTime start = LocalDate.now().plusDays(1).atTime(10, 0);
        LocalDateTime end = start.plusHours(3);
        CreateBookingRequest request = new CreateBookingRequest(3L, 7L, start, end);

        UserEntity user = new UserEntity();
        user.setUserId(11L);
        user.setUserEmail("client@example.com");

        LocationEntity location = new LocationEntity();
        location.setLocationId(3L);
        location.setLocationName("Зал «Волга»");
        location.setLocationAddress("Самара, ул. Молодогвардейская, 151");
        location.setLocationOpening(LocalTime.of(8, 0));
        location.setLocationClosing(LocalTime.of(22, 0));

        TariffEntity tariff = new TariffEntity();
        tariff.setTariffId(7L);
        tariff.setTariffName("Базовый");
        tariff.setTariffBasePrice(new BigDecimal("1000.00"));
        tariff.setTariffDiscount(new BigDecimal("10.00"));
        tariff.setTariffStatus("Действует");

        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        when(locationRepository.findById(3L)).thenReturn(Optional.of(location));
        when(tariffRepository.findById(7L)).thenReturn(Optional.of(tariff));
        when(bookingRepository.existsConflictingBooking(eq(3L), eq(BookingStatus.FINISHED), eq(start), eq(end))).thenReturn(false);
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(invocation -> {
            BookingEntity booking = invocation.getArgument(0);
            booking.setBookingId(101L);
            return booking;
        });
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> {
            PaymentEntity payment = invocation.getArgument(0);
            payment.setPaymentId(202L);
            return payment;
        });

        BookingResponse response = bookingService.createBooking(11L, request);

        ArgumentCaptor<BookingEntity> bookingCaptor = ArgumentCaptor.forClass(BookingEntity.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        BookingEntity savedBooking = bookingCaptor.getValue();
        assertEquals(3, savedBooking.getBookingDuration());
        assertEquals(new BigDecimal("2700.00"), savedBooking.getBookingPrice());
        assertEquals(BookingStatus.UNCONFIRMED, savedBooking.getBookingStatus());

        ArgumentCaptor<PaymentEntity> paymentCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        PaymentEntity savedPayment = paymentCaptor.getValue();
        assertEquals(new BigDecimal("2700.00"), savedPayment.getPaymentSum());
        assertEquals(PaymentStatus.UNPAID, savedPayment.getPaymentStatus());
        assertEquals("Онлайн", savedPayment.getPaymentMethod());
        assertEquals(start.toLocalDate(), savedPayment.getPaymentDueDate());

        ArgumentCaptor<BookingPaymentLinkEntity> linkCaptor = ArgumentCaptor.forClass(BookingPaymentLinkEntity.class);
        verify(bookingPaymentLinkRepository).save(linkCaptor.capture());
        assertEquals(101L, linkCaptor.getValue().getBookingId());
        assertEquals(202L, linkCaptor.getValue().getPaymentId());

        assertEquals(101L, response.bookingId());
        assertEquals("2700.00", response.bookingPrice().toPlainString());
        assertEquals("Unconfirmed", response.bookingStatus());
        assertEquals(202L, response.payment().paymentId());
    }

    @Test
    void createBookingThrowsConflictForOccupiedInterval() {
        LocalDateTime start = LocalDate.now().plusDays(1).atTime(12, 0);
        LocalDateTime end = start.plusHours(2);
        CreateBookingRequest request = new CreateBookingRequest(3L, 7L, start, end);

        UserEntity user = new UserEntity();
        user.setUserId(11L);

        LocationEntity location = new LocationEntity();
        location.setLocationId(3L);
        location.setLocationOpening(LocalTime.of(8, 0));
        location.setLocationClosing(LocalTime.of(22, 0));

        TariffEntity tariff = new TariffEntity();
        tariff.setTariffId(7L);
        tariff.setTariffBasePrice(new BigDecimal("1000.00"));
        tariff.setTariffDiscount(BigDecimal.ZERO);
        tariff.setTariffStatus("Доступен");

        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        when(locationRepository.findById(3L)).thenReturn(Optional.of(location));
        when(tariffRepository.findById(7L)).thenReturn(Optional.of(tariff));
        when(bookingRepository.existsConflictingBooking(eq(3L), eq(BookingStatus.FINISHED), eq(start), eq(end))).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> bookingService.createBooking(11L, request));

        assertEquals(CONFLICT, exception.getStatus());
        assertEquals("Выбранный интервал уже занят.", exception.getMessage());
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
        verify(bookingPaymentLinkRepository, never()).save(any(BookingPaymentLinkEntity.class));
    }

    @Test
    void createBookingThrowsBadRequestForUnavailableTariff() {
        LocalDateTime start = LocalDate.now().plusDays(1).atTime(12, 0);
        LocalDateTime end = start.plusHours(2);
        CreateBookingRequest request = new CreateBookingRequest(3L, 7L, start, end);

        UserEntity user = new UserEntity();
        user.setUserId(11L);

        LocationEntity location = new LocationEntity();
        location.setLocationId(3L);
        location.setLocationOpening(LocalTime.of(8, 0));
        location.setLocationClosing(LocalTime.of(22, 0));

        TariffEntity tariff = new TariffEntity();
        tariff.setTariffId(7L);
        tariff.setTariffBasePrice(new BigDecimal("1000.00"));
        tariff.setTariffDiscount(BigDecimal.ZERO);
        tariff.setTariffStatus("Недоступен");

        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        when(locationRepository.findById(3L)).thenReturn(Optional.of(location));
        when(tariffRepository.findById(7L)).thenReturn(Optional.of(tariff));

        ApiException exception = assertThrows(ApiException.class, () -> bookingService.createBooking(11L, request));

        assertEquals(BAD_REQUEST, exception.getStatus());
        assertEquals("Этот тариф сейчас недоступен для бронирования.", exception.getMessage());
        verify(bookingRepository, never()).existsConflictingBooking(any(), any(), any(), any());
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
        verify(bookingPaymentLinkRepository, never()).save(any(BookingPaymentLinkEntity.class));
    }
}
