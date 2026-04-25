package ICT.project.BookingService.entity;

import jakarta.persistence.EnumeratedValue;
import java.util.Arrays;

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

    public static BookingStatus fromValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(value) || status.dbValue.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Неизвестный статус бронирования: " + value));
    }
}
