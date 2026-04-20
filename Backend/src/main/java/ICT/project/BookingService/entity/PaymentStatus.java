package ICT.project.BookingService.entity;

import jakarta.persistence.EnumeratedValue;

public enum PaymentStatus {
    UNPAID("Unpaid"),
    PAID("Paid"),
    PROCESSING("Processing");

    @EnumeratedValue
    private final String dbValue;

    PaymentStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }
}
