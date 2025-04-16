package dev.yerid.model.financeManager.expenses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class Expenses {
    private String id;
    private String nombreG;
    private double gasto;
    private String categoria;
    private Long fecha;
    private String userId;
    private String origen;
    private String recordatorioId;
}
