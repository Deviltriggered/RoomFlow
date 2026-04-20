package ICT.project.BookingService.config;

import ICT.project.BookingService.entity.LocationEntity;
import ICT.project.BookingService.entity.TariffEntity;
import ICT.project.BookingService.repository.LocationRepository;
import ICT.project.BookingService.repository.TariffRepository;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedReferenceData(LocationRepository locationRepository, TariffRepository tariffRepository) {
        return args -> {
            if (locationRepository.count() == 0) {
                locationRepository.save(createLocation(
                        "Конференц-зал",
                        "Зал «Волга»",
                        "Самара, ул. Молодогвардейская, 151",
                        LocalTime.of(8, 0),
                        LocalTime.of(22, 0),
                        "+7 (846) 200-10-10",
                        new BigDecimal("53.195878"),
                        new BigDecimal("50.101783")
                ));
                locationRepository.save(createLocation(
                        "Рабочее пространство",
                        "Пространство «Маяк»",
                        "Самара, Московское шоссе, 4",
                        LocalTime.of(9, 0),
                        LocalTime.of(21, 0),
                        "+7 (846) 200-20-20",
                        new BigDecimal("53.204512"),
                        new BigDecimal("50.179806")
                ));
                locationRepository.save(createLocation(
                        "Переговорная",
                        "Комната «Спутник»",
                        "Самара, ул. Гагарина, 96",
                        LocalTime.of(10, 0),
                        LocalTime.of(20, 0),
                        "+7 (846) 200-30-30",
                        new BigDecimal("53.209455"),
                        new BigDecimal("50.182092")
                ));
            }

            if (tariffRepository.count() == 0) {
                tariffRepository.save(createTariff("Базовый", "Стандартный", new BigDecimal("1200.00"), new BigDecimal("0.00"), "Действует"));
                tariffRepository.save(createTariff("Рабочий день", "Дневной", new BigDecimal("900.00"), new BigDecimal("10.00"), "Недоступен"));
                tariffRepository.save(createTariff("Старт", "Пробный", new BigDecimal("700.00"), new BigDecimal("15.00"), "Действует"));
            }

            tariffRepository.findAll().stream()
                    .filter(this::isBusinessDayTariff)
                    .filter(tariff -> !"Недоступен".equalsIgnoreCase(tariff.getTariffStatus()))
                    .forEach(tariff -> {
                        tariff.setTariffStatus("Недоступен");
                        tariffRepository.save(tariff);
                    });
        };
    }

    private LocationEntity createLocation(
            String type,
            String name,
            String address,
            LocalTime opening,
            LocalTime closing,
            String phone,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        LocationEntity location = new LocationEntity();
        location.setLocationType(type);
        location.setLocationName(name);
        location.setLocationAddress(address);
        location.setLocationOpening(opening);
        location.setLocationClosing(closing);
        location.setLocationPhone(phone);
        location.setLocationLatitude(latitude);
        location.setLocationLongitude(longitude);
        return location;
    }

    private TariffEntity createTariff(String name, String type, BigDecimal basePrice, BigDecimal discount, String status) {
        TariffEntity tariff = new TariffEntity();
        tariff.setTariffName(name);
        tariff.setTariffType(type);
        tariff.setTariffBasePrice(basePrice);
        tariff.setTariffDiscount(discount);
        tariff.setTariffStatus(status);
        return tariff;
    }

    private boolean isBusinessDayTariff(TariffEntity tariff) {
        String tariffName = tariff.getTariffName();
        return "Рабочий день".equalsIgnoreCase(tariffName)
                || "Business Day".equalsIgnoreCase(tariffName)
                || "Business".equalsIgnoreCase(tariffName);
    }
}
