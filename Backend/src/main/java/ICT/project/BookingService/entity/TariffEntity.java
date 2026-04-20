package ICT.project.BookingService.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "tariffs")
public class TariffEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tariff_id")
    private Long tariffId;

    @Column(name = "tariff_name", nullable = false)
    private String tariffName;

    @Column(name = "tariff_type", nullable = false)
    private String tariffType;

    @Column(name = "tariff_base_price", nullable = false)
    private BigDecimal tariffBasePrice;

    @Column(name = "tariff_discount", nullable = false)
    private BigDecimal tariffDiscount;

    @Column(name = "tariff_status", nullable = false)
    private String tariffStatus;

    public Long getTariffId() {
        return tariffId;
    }

    public void setTariffId(Long tariffId) {
        this.tariffId = tariffId;
    }

    public String getTariffName() {
        return tariffName;
    }

    public void setTariffName(String tariffName) {
        this.tariffName = tariffName;
    }

    public String getTariffType() {
        return tariffType;
    }

    public void setTariffType(String tariffType) {
        this.tariffType = tariffType;
    }

    public BigDecimal getTariffBasePrice() {
        return tariffBasePrice;
    }

    public void setTariffBasePrice(BigDecimal tariffBasePrice) {
        this.tariffBasePrice = tariffBasePrice;
    }

    public BigDecimal getTariffDiscount() {
        return tariffDiscount;
    }

    public void setTariffDiscount(BigDecimal tariffDiscount) {
        this.tariffDiscount = tariffDiscount;
    }

    public String getTariffStatus() {
        return tariffStatus;
    }

    public void setTariffStatus(String tariffStatus) {
        this.tariffStatus = tariffStatus;
    }
}
