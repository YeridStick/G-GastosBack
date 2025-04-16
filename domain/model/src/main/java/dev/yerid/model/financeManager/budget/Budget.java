package dev.yerid.model.financeManager.budget;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class Budget {
    private String id;
    private String userId;
    private double monto;
    private Long fechaActualizacion;
}