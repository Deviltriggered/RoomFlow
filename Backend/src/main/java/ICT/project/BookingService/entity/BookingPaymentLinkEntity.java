package ICT.project.BookingService.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "booking_payment_links")
public class BookingPaymentLinkEntity {

    @Id
    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "payment_id", nullable = false, unique = true)
    private Long paymentId;

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }
}
