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
@Document(collection = "budgets")
public class BudgetData {
    @Id
    private String id;
    private String userId;
    private double monto;
    private Long fechaActualizacion;
}