package dev.yerid.mongodb.data.financeManager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "savingsGoals")
public class SavingsGoalData {
    @Id
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