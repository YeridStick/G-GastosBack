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
@Document(collection = "extraIncome")
public class ExtraIncomeData {
    @Id
    private String id;
    private double monto;
    private String descripcion;
    private Long fecha;
    private String userId;
}
