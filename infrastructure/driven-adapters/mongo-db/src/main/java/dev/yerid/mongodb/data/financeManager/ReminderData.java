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
@Document(collection = "reminders")
public class ReminderData {
    @Id
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