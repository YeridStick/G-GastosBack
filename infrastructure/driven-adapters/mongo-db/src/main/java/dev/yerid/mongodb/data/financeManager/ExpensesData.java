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
@Document(collection = "expenses")
public class ExpensesData {
    @Id
    private String id;
    private String nombreG;
    private double gasto;
    private String categoria;
    private Long fecha;
    private String userId;
    private String origen;
    private String recordatorioId;
}