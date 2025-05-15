package dev.yerid.model.financeManager.finance;
import dev.yerid.model.financeManager.budget.Budget;
import dev.yerid.model.financeManager.categories.Categories;
import dev.yerid.model.financeManager.expenses.Expenses;
import dev.yerid.model.financeManager.extraincome.ExtraIncome;
import dev.yerid.model.financeManager.reminder.Reminder;
import dev.yerid.model.financeManager.savingsgoal.SavingsGoal;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
//import lombok.NoArgsConstructor;


@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class FinanceManager {
    private String id;
    private String userId;

    // Campos para seguimiento y sincronización
    private Long lastSyncTimestamp;
    private String lastVisitedRoute;
    private String sessionId;
    private Long dataImportTimestamp;

    // Datos financieros
    private Budget presupuesto;
    private Map<String, Expenses> gastos;
    private Map<String, Categories> categorias;
    private Map<String, SavingsGoal> metasAhorro;
    private Map<String, Reminder> recordatorios;
    private Map<String, ExtraIncome> ingresosExtra;

    // Elementos eliminados
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
}
