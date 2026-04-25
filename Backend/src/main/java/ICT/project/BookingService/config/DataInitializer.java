package ICT.project.BookingService.config;

import ICT.project.BookingService.entity.LocationEntity;
import ICT.project.BookingService.entity.TariffEntity;
import ICT.project.BookingService.entity.UserEntity;
import ICT.project.BookingService.repository.LocationRepository;
import ICT.project.BookingService.repository.TariffRepository;
import ICT.project.BookingService.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    private final String adminEmail;

    public DataInitializer(@Value("${app.auth.admin-email:}") String adminEmail) {
        this.adminEmail = adminEmail == null ? "" : adminEmail.trim().toLowerCase();
    }

    @Bean
    public CommandLineRunner seedReferenceData(
            LocationRepository locationRepository,
            TariffRepository tariffRepository,
            UserRepository userRepository
    ) {
        return args -> {
            if (locationRepository.count() == 0) {
                locationRepository.save(createLocation(
                        "Конференц-зал",
                        "Зал «Орион»",
                        "Деловой центр, корпус A",
                        LocalTime.of(8, 0),
                        LocalTime.of(22, 0),
                        "+7 (000) 100-10-10",
                        null,
                        null
                ));
                locationRepository.save(createLocation(
                        "Рабочее пространство",
                        "Пространство «Атлас»",
                        "Деловой центр, корпус B",
                        LocalTime.of(9, 0),
                        LocalTime.of(21, 0),
                        "+7 (000) 100-20-20",
                        null,
                        null
                ));
                locationRepository.save(createLocation(
                        "Переговорная",
                        "Комната «Навигатор»",
                        "Деловой центр, корпус C",
                        LocalTime.of(10, 0),
                        LocalTime.of(20, 0),
                        "+7 (000) 100-30-30",
                        null,
                        null
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

            if (adminEmail.isBlank()) {
                return;
            }

            Optional.ofNullable(userRepository.findByUserEmailIgnoreCase(adminEmail))
                    .orElse(Optional.empty())
                    .map(this::promoteAdmin)
                    .ifPresent(userRepository::save);
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

    private UserEntity promoteAdmin(UserEntity user) {
        user.setUserRole("ADMIN");
        return user;
    }
}
