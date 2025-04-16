package dev.yerid.model.financeManager.reminder;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class Reminder {
    private String id;
    private String titulo;
    private String descripcion;
    private double monto;
    private Long fechaVencimiento;
    private String categoria;
    private boolean esRecurrente;
    private String frecuencia;
    private int diasAnticipacion;
    private Long fechaCreacion;
    private String estado;
    private String userId;
}
