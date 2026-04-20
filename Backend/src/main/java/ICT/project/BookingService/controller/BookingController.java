package ICT.project.BookingService.controller;

import ICT.project.BookingService.dto.BookingResponse;
import ICT.project.BookingService.dto.CreateBookingRequest;
import ICT.project.BookingService.dto.PaymentSummaryResponse;
import ICT.project.BookingService.security.AuthenticatedUser;
import ICT.project.BookingService.service.BookingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/bookings")
    public BookingResponse createBooking(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateBookingRequest request
    ) {
        return bookingService.createBooking(user.userId(), request);
    }

    @GetMapping("/bookings/my")
    public List<BookingResponse> getMyBookings(@AuthenticationPrincipal AuthenticatedUser user) {
        return bookingService.getUserBookings(user.userId());
    }

    @GetMapping("/payments/my")
    public List<PaymentSummaryResponse> getMyPayments(@AuthenticationPrincipal AuthenticatedUser user) {
        return bookingService.getUserPayments(user.userId());
    }
}
