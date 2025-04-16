package dev.yerid.model.financeManager.savingsgoal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class SavingsGoal {
    private String id;
    private String nombre;
    private double monto;
    private String fechaObjetivo;
    private String descripcion;
    private Long creada;
    private double ahorroAcumulado;
    private double ahorroSemanal;
    private double ahorroMensual;
    private double ahorroAnual;
    private int diasRestantes;
    private boolean completada;
    private String userId;
}
