package ICT.project.BookingService.service;

import ICT.project.BookingService.dto.LocationResponse;
import ICT.project.BookingService.dto.LocationAvailabilityResponse;
import ICT.project.BookingService.dto.TariffResponse;
import ICT.project.BookingService.entity.BookingEntity;
import ICT.project.BookingService.entity.BookingStatus;
import ICT.project.BookingService.entity.LocationEntity;
import ICT.project.BookingService.entity.TariffEntity;
import ICT.project.BookingService.repository.BookingRepository;
import ICT.project.BookingService.repository.LocationRepository;
import ICT.project.BookingService.repository.TariffRepository;
import ICT.project.BookingService.support.ApiException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

    private final LocationRepository locationRepository;
    private final TariffRepository tariffRepository;
    private final BookingRepository bookingRepository;

    public CatalogService(
            LocationRepository locationRepository,
            TariffRepository tariffRepository,
            BookingRepository bookingRepository
    ) {
        this.locationRepository = locationRepository;
        this.tariffRepository = tariffRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<LocationResponse> getLocations() {
        return locationRepository.findAllByOrderByLocationNameAsc().stream().map(this::toLocationResponse).toList();
    }

    public List<TariffResponse> getTariffs() {
        return tariffRepository.findAllByOrderByTariffBasePriceAsc().stream().map(this::toTariffResponse).toList();
    }

    public LocationAvailabilityResponse getLocationAvailability(Long locationId, LocalDate date) {
        LocationEntity location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Помещение не найдено."));

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        List<BookingEntity> bookings = bookingRepository.findAllActiveByLocationAndDate(
                locationId,
                BookingStatus.FINISHED,
                dayStart,
                dayEnd
        );

        Set<Integer> occupiedHours = new LinkedHashSet<>();
        for (BookingEntity booking : bookings) {
            LocalDateTime start = booking.getBookingStart().isBefore(dayStart) ? dayStart : booking.getBookingStart();
            LocalDateTime end = booking.getBookingEnd().isAfter(dayEnd) ? dayEnd : booking.getBookingEnd();

            LocalDateTime slotStart = start.truncatedTo(ChronoUnit.HOURS);
            while (slotStart.isBefore(end)) {
                occupiedHours.add(slotStart.getHour());
                slotStart = slotStart.plusHours(1);
            }
        }

        return new LocationAvailabilityResponse(location.getLocationId(), date, occupiedHours.stream().sorted().toList());
    }

    public LocationResponse toLocationResponse(LocationEntity location) {
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

    public TariffResponse toTariffResponse(TariffEntity tariff) {
        return new TariffResponse(
                tariff.getTariffId(),
                tariff.getTariffName(),
                tariff.getTariffType(),
                tariff.getTariffBasePrice(),
                tariff.getTariffDiscount(),
                tariff.getTariffStatus()
        );
    }
}
