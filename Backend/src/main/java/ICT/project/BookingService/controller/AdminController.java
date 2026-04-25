package ICT.project.BookingService.controller;

import ICT.project.BookingService.dto.AdminBookingResponse;
import ICT.project.BookingService.dto.LocationResponse;
import ICT.project.BookingService.dto.UpdateBookingStatusRequest;
import ICT.project.BookingService.dto.UpsertLocationRequest;
import ICT.project.BookingService.service.AdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/bookings")
    public List<AdminBookingResponse> getAllBookings() {
        return adminService.getAllBookings();
    }

    @PatchMapping("/bookings/{bookingId}/status")
    public AdminBookingResponse updateBookingStatus(
            @PathVariable Long bookingId,
            @Valid @RequestBody UpdateBookingStatusRequest request
    ) {
        return adminService.updateBookingStatus(bookingId, request);
    }

    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long bookingId) {
        adminService.deleteBooking(bookingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/locations")
    public LocationResponse createLocation(@Valid @RequestBody UpsertLocationRequest request) {
        return adminService.createLocation(request);
    }

    @PutMapping("/locations/{locationId}")
    public LocationResponse updateLocation(
            @PathVariable Long locationId,
            @Valid @RequestBody UpsertLocationRequest request
    ) {
        return adminService.updateLocation(locationId, request);
    }
}
