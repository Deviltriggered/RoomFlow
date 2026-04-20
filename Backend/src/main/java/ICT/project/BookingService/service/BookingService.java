package ICT.project.BookingService.service;

import ICT.project.BookingService.dto.BookingResponse;
import ICT.project.BookingService.dto.CreateBookingRequest;
import ICT.project.BookingService.dto.PaymentResponse;
import ICT.project.BookingService.dto.PaymentSummaryResponse;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BookingPaymentLinkRepository bookingPaymentLinkRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final TariffRepository tariffRepository;

    public BookingService(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            BookingPaymentLinkRepository bookingPaymentLinkRepository,
            UserRepository userRepository,
            LocationRepository locationRepository,
            TariffRepository tariffRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.bookingPaymentLinkRepository = bookingPaymentLinkRepository;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.tariffRepository = tariffRepository;
    }

    @Transactional
    public BookingResponse createBooking(Long userId, CreateBookingRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Пользователь не найден."));
        LocationEntity location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Помещение не найдено."));
        TariffEntity tariff = tariffRepository.findById(request.tariffId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Тариф не найден."));

        validateBookingWindow(request.bookingStart(), request.bookingEnd(), location);
        validateTariffStatus(tariff);

        if (bookingRepository.existsConflictingBooking(
                location.getLocationId(),
                BookingStatus.FINISHED,
                request.bookingStart(),
                request.bookingEnd()
        )) {
            throw new ApiException(HttpStatus.CONFLICT, "Выбранный интервал уже занят.");
        }

        int hours = Math.toIntExact(Duration.between(request.bookingStart(), request.bookingEnd()).toHours());
        BigDecimal totalPrice = calculatePrice(tariff, hours);

        BookingEntity booking = new BookingEntity();
        booking.setUser(user);
        booking.setLocation(location);
        booking.setTariff(tariff);
        booking.setBookingStart(request.bookingStart());
        booking.setBookingEnd(request.bookingEnd());
        booking.setBookingDuration(hours);
        booking.setBookingPrice(totalPrice);
        booking.setBookingStatus(BookingStatus.UNCONFIRMED);
        booking = bookingRepository.save(booking);

        PaymentEntity payment = new PaymentEntity();
        payment.setUser(user);
        payment.setTariff(tariff);
        payment.setPaymentSum(totalPrice);
        payment.setPaymentStatus(PaymentStatus.UNPAID);
        payment.setPaymentMethod("Онлайн");
        payment.setPaymentCreatedAt(LocalDateTime.now());
        payment.setPaymentDueDate(request.bookingStart().toLocalDate());
        payment = paymentRepository.save(payment);

        BookingPaymentLinkEntity link = new BookingPaymentLinkEntity();
        link.setBookingId(booking.getBookingId());
        link.setPaymentId(payment.getPaymentId());
        bookingPaymentLinkRepository.save(link);

        return toBookingResponse(booking, payment);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(Long userId) {
        List<BookingEntity> bookings = bookingRepository.findAllByUserUserIdOrderByBookingStartDesc(userId);
        if (bookings.isEmpty()) {
            return List.of();
        }

        Map<Long, BookingPaymentLinkEntity> linksByBookingId = bookingPaymentLinkRepository.findAllByBookingIds(
                bookings.stream().map(BookingEntity::getBookingId).toList()
        ).stream().collect(Collectors.toMap(BookingPaymentLinkEntity::getBookingId, Function.identity()));

        Map<Long, PaymentEntity> paymentsById = paymentRepository.findAllById(
                linksByBookingId.values().stream().map(BookingPaymentLinkEntity::getPaymentId).toList()
        ).stream().collect(Collectors.toMap(PaymentEntity::getPaymentId, Function.identity()));

        return bookings.stream()
                .map(booking -> toBookingResponse(booking, paymentsById.get(linksByBookingId.get(booking.getBookingId()).getPaymentId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentSummaryResponse> getUserPayments(Long userId) {
        List<PaymentEntity> payments = paymentRepository.findAllByUserUserIdOrderByPaymentCreatedAtDesc(userId);
        if (payments.isEmpty()) {
            return List.of();
        }

        Map<Long, BookingPaymentLinkEntity> linksByPaymentId = bookingPaymentLinkRepository.findAllByPaymentIds(
                payments.stream().map(PaymentEntity::getPaymentId).toList()
        ).stream().collect(Collectors.toMap(BookingPaymentLinkEntity::getPaymentId, Function.identity()));

        Map<Long, BookingEntity> bookingsById = new HashMap<>();
        List<Long> bookingIds = linksByPaymentId.values().stream().map(BookingPaymentLinkEntity::getBookingId).toList();
        if (!bookingIds.isEmpty()) {
            bookingsById.putAll(bookingRepository.findAllById(bookingIds).stream()
                    .collect(Collectors.toMap(BookingEntity::getBookingId, Function.identity())));
        }

        return payments.stream().map(payment -> {
            BookingPaymentLinkEntity link = linksByPaymentId.get(payment.getPaymentId());
            BookingEntity booking = link == null ? null : bookingsById.get(link.getBookingId());
            return new PaymentSummaryResponse(
                    payment.getPaymentId(),
                    booking == null ? null : booking.getBookingId(),
                    booking == null ? null : booking.getLocation().getLocationName(),
                    payment.getTariff().getTariffName(),
                    payment.getPaymentSum(),
                    payment.getPaymentStatus().getDbValue(),
                    payment.getPaymentMethod(),
                    payment.getPaymentCreatedAt(),
                    payment.getPaymentDueDate()
            );
        }).toList();
    }

    private void validateBookingWindow(LocalDateTime bookingStart, LocalDateTime bookingEnd, LocationEntity location) {
        if (!bookingStart.isBefore(bookingEnd)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Время окончания должно быть позже времени начала.");
        }
        if (!bookingStart.toLocalDate().equals(bookingEnd.toLocalDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Бронирование должно укладываться в один день.");
        }
        if (bookingStart.getMinute() != 0 || bookingStart.getSecond() != 0 || bookingStart.getNano() != 0
                || bookingEnd.getMinute() != 0 || bookingEnd.getSecond() != 0 || bookingEnd.getNano() != 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Бронирование доступно только на целые часы.");
        }
        long hours = Duration.between(bookingStart, bookingEnd).toHours();
        if (hours <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Минимальная длительность аренды — один час.");
        }
        if (bookingStart.toLocalTime().isBefore(location.getLocationOpening())
                || bookingEnd.toLocalTime().isAfter(location.getLocationClosing())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Выбранное время выходит за рамки графика работы помещения."
            );
        }
        if (bookingStart.isBefore(LocalDateTime.now().withMinute(0).withSecond(0).withNano(0))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Время начала должно быть в будущем.");
        }
    }

    private void validateTariffStatus(TariffEntity tariff) {
        String tariffStatus = tariff.getTariffStatus();
        boolean isAvailable = "active".equalsIgnoreCase(tariffStatus)
                || "действует".equalsIgnoreCase(tariffStatus)
                || "доступен".equalsIgnoreCase(tariffStatus);
        if (!isAvailable) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Этот тариф сейчас недоступен для бронирования.");
        }
    }

    private BigDecimal calculatePrice(TariffEntity tariff, int hours) {
        BigDecimal base = tariff.getTariffBasePrice().multiply(BigDecimal.valueOf(hours));
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                tariff.getTariffDiscount().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
        );
        return base.multiply(discountMultiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private BookingResponse toBookingResponse(BookingEntity booking, PaymentEntity payment) {
        return new BookingResponse(
                booking.getBookingId(),
                booking.getLocation().getLocationId(),
                booking.getLocation().getLocationName(),
                booking.getLocation().getLocationAddress(),
                booking.getTariff().getTariffId(),
                booking.getTariff().getTariffName(),
                booking.getBookingStart(),
                booking.getBookingEnd(),
                booking.getBookingDuration(),
                booking.getBookingPrice(),
                booking.getBookingStatus().getDbValue(),
                toPaymentResponse(payment)
        );
    }

    private PaymentResponse toPaymentResponse(PaymentEntity payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getPaymentSum(),
                payment.getPaymentStatus().getDbValue(),
                payment.getPaymentMethod(),
                payment.getPaymentCreatedAt(),
                payment.getPaymentDueDate()
        );
    }
}
