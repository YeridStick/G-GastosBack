package dev.yerid.model.financeManager.extraincome;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class ExtraIncome {
    private String id;
    private double monto;
    private String descripcion;
    private Long fecha;
    private String userId;
}