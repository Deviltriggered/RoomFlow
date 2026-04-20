package ICT.project.BookingService.controller;

import ICT.project.BookingService.dto.LocationResponse;
import ICT.project.BookingService.dto.LocationAvailabilityResponse;
import ICT.project.BookingService.dto.TariffResponse;
import java.time.LocalDate;
import ICT.project.BookingService.service.CatalogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/locations")
    public List<LocationResponse> getLocations() {
        return catalogService.getLocations();
    }

    @GetMapping("/locations/{locationId}/availability")
    public LocationAvailabilityResponse getLocationAvailability(
            @PathVariable Long locationId,
            @RequestParam LocalDate date
    ) {
        return catalogService.getLocationAvailability(locationId, date);
    }

    @GetMapping("/tariffs")
    public List<TariffResponse> getTariffs() {
        return catalogService.getTariffs();
    }

    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}
