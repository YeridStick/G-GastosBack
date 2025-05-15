package dev.yerid.mongodb.data.financeManager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "financeManager")
public class FinanceManagerData {
    @Id
    private String id; // Usaremos el userId o email como ID del documento

    @Indexed(unique = true)
    private String userId;

    // Campos para seguimiento y sincronización
    private Long lastSyncTimestamp;
    private String lastVisitedRoute;
    private String sessionId;
    private Long dataImportTimestamp;

    // Presupuesto
    private BudgetData presupuesto;

    // Colecciones de elementos financieros usando mapas
    // para optimizar actualizaciones parciales
    private Map<String, ExpensesData> gastos;
    private Map<String, CategoriesData> categorias;
    private Map<String, SavingsGoalData> metasAhorro;
    private Map<String, ReminderData> recordatorios;
    private Map<String, ExtraIncomeData> ingresosExtra;

    // Información de elementos eliminados
    private EliminadosInfo eliminados;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EliminadosInfo {
        private List<String> gastos;
        private List<String> categorias;
        private List<String> metasAhorro;
        private List<String> recordatorios;
        private List<String> ingresosExtra;
    }

    /**
     * Crea una instancia vacía de FinanceManagerData para un usuario
     */
    public static FinanceManagerData createEmpty(String userId) {
        String docId = userId; // Usamos el userId como ID del documento

        return FinanceManagerData.builder()
                .id(docId)
                .userId(userId)
                .lastSyncTimestamp(System.currentTimeMillis())
                .presupuesto(new BudgetData(docId, 0, System.currentTimeMillis()))
                .gastos(new HashMap<>())
                .categorias(new HashMap<>())
                .metasAhorro(new HashMap<>())
                .recordatorios(new HashMap<>())
                .ingresosExtra(new HashMap<>())
                .eliminados(EliminadosInfo.builder()
                        .gastos(new ArrayList<>())
                        .categorias(new ArrayList<>())
                        .metasAhorro(new ArrayList<>())
                        .recordatorios(new ArrayList<>())
                        .ingresosExtra(new ArrayList<>())
                        .build())
                .build();
    }
}