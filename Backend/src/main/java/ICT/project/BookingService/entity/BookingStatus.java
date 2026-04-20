package ICT.project.BookingService.entity;

import jakarta.persistence.EnumeratedValue;

public enum BookingStatus {
    CONFIRMED("Confirmed"),
    UNCONFIRMED("Unconfirmed"),
    IN_PROGRESS("In progress"),
    FINISHED("Finished");

    @EnumeratedValue
    private final String dbValue;

    BookingStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }
}
