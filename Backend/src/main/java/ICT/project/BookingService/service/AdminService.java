package ICT.project.BookingService.service;

import ICT.project.BookingService.dto.AdminBookingResponse;
import ICT.project.BookingService.dto.LocationResponse;
import ICT.project.BookingService.dto.PaymentResponse;
import ICT.project.BookingService.dto.UpdateBookingStatusRequest;
import ICT.project.BookingService.dto.UpsertLocationRequest;
import ICT.project.BookingService.entity.BookingEntity;
import ICT.project.BookingService.entity.BookingPaymentLinkEntity;
import ICT.project.BookingService.entity.BookingStatus;
import ICT.project.BookingService.entity.LocationEntity;
import ICT.project.BookingService.entity.PaymentEntity;
import ICT.project.BookingService.repository.BookingPaymentLinkRepository;
import ICT.project.BookingService.repository.BookingRepository;
import ICT.project.BookingService.repository.LocationRepository;
import ICT.project.BookingService.repository.PaymentRepository;
import ICT.project.BookingService.support.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final BookingRepository bookingRepository;
    private final BookingPaymentLinkRepository bookingPaymentLinkRepository;
    private final PaymentRepository paymentRepository;
    private final LocationRepository locationRepository;

    public AdminService(
            BookingRepository bookingRepository,
            BookingPaymentLinkRepository bookingPaymentLinkRepository,
            PaymentRepository paymentRepository,
            LocationRepository locationRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingPaymentLinkRepository = bookingPaymentLinkRepository;
        this.paymentRepository = paymentRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminBookingResponse> getAllBookings() {
        return bookingRepository.findAllByOrderByBookingStartDesc().stream()
                .map(this::toAdminBookingResponse)
                .toList();
    }

    @Transactional
    public AdminBookingResponse updateBookingStatus(Long bookingId, UpdateBookingStatusRequest request) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Бронирование не найдено."));

        try {
            booking.setBookingStatus(BookingStatus.fromValue(request.status()));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
        bookingRepository.save(booking);
        return toAdminBookingResponse(booking);
    }

    @Transactional
    public void deleteBooking(Long bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Бронирование не найдено."));

        bookingPaymentLinkRepository.findById(bookingId)
                .ifPresent(link -> paymentRepository.deleteById(link.getPaymentId()));

        bookingRepository.delete(booking);
    }

    @Transactional
    public LocationResponse createLocation(UpsertLocationRequest request) {
        validateSchedule(request.opening(), request.closing());

        LocationEntity location = new LocationEntity();
        applyLocationChanges(location, request);
        location = locationRepository.save(location);
        return toLocationResponse(location);
    }

    @Transactional
    public LocationResponse updateLocation(Long locationId, UpsertLocationRequest request) {
        validateSchedule(request.opening(), request.closing());

        LocationEntity location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Помещение не найдено."));

        applyLocationChanges(location, request);
        location = locationRepository.save(location);
        return toLocationResponse(location);
    }

    private void applyLocationChanges(LocationEntity location, UpsertLocationRequest request) {
        location.setLocationType(request.type().trim());
        location.setLocationName(request.name().trim());
        location.setLocationAddress(request.address().trim());
        location.setLocationOpening(request.opening());
        location.setLocationClosing(request.closing());
        location.setLocationPhone(blankToNull(request.phone()));
        location.setLocationLatitude(request.latitude());
        location.setLocationLongitude(request.longitude());
    }

    private void validateSchedule(java.time.LocalTime opening, java.time.LocalTime closing) {
        if (!opening.isBefore(closing)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Время открытия должно быть раньше времени закрытия.");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private AdminBookingResponse toAdminBookingResponse(BookingEntity booking) {
        BookingPaymentLinkEntity link = bookingPaymentLinkRepository.findById(booking.getBookingId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Для бронирования #" + booking.getBookingId() + " не найден связанный платёж."
                ));
        PaymentEntity payment = paymentRepository.findById(link.getPaymentId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Платёж #" + link.getPaymentId() + " не найден."
                ));

        return new AdminBookingResponse(
                booking.getBookingId(),
                booking.getUser().getUserId(),
                booking.getUser().getUserEmail(),
                booking.getUser().getUserLegalName(),
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

    private LocationResponse toLocationResponse(LocationEntity location) {
        return new LocationResponse(
                location.getLocationId(),
                location.getLocationType(),
                location.getLocationName(),
                location.getLocationAddress(),
                location.getLocationOpening(),
                location.getLocationClosing(),
                location.getLocationPhone(),
                location.getLocationLatitude(),
                location.getLocationLongitude()
        );
    }
}
