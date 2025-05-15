package dev.yerid.mongodb.adapter.financeManegerAdapters;

import dev.yerid.model.financeManager.budget.Budget;
import dev.yerid.model.financeManager.categories.Categories;
import dev.yerid.model.financeManager.expenses.Expenses;
import dev.yerid.model.financeManager.extraincome.ExtraIncome;
import dev.yerid.model.financeManager.finance.FinanceManager;
import dev.yerid.model.financeManager.reminder.Reminder;
import dev.yerid.model.financeManager.savingsgoal.SavingsGoal;
import dev.yerid.mongodb.data.financeManager.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Clase utilitaria para convertir entre objetos de dominio y objetos de datos
 */
@Component
public class FinanceManagerDataConverter {

    /**
     * Convierte un objeto FinanceManagerData a FinanceManager
     */
    public FinanceManager toEntity(FinanceManagerData data) {
        if (data == null) {
            return null;
        }

        // Convertir el presupuesto
        Budget presupuesto = convertToBudget(data.getPresupuesto(), data.getUserId());

        // Convertir los gastos
        Map<String, Expenses> gastos = convertToExpensesMap(data.getGastos(), data.getUserId());

        // Convertir las categorías
        Map<String, Categories> categorias = convertToCategoriesMap(data.getCategorias(), data.getUserId());

        // Convertir las metas de ahorro
        Map<String, SavingsGoal> metasAhorro = convertToSavingsGoalMap(data.getMetasAhorro(), data.getUserId());

        // Convertir los recordatorios
        Map<String, Reminder> recordatorios = convertToReminderMap(data.getRecordatorios(), data.getUserId());

        // Convertir los ingresos extra
        Map<String, ExtraIncome> ingresosExtra = convertToExtraIncomeMap(data.getIngresosExtra(), data.getUserId());

        // Convertir los elementos eliminados
        FinanceManager.EliminadosInfo eliminadosEntity = convertToEliminadosInfo(data.getEliminados());

        return FinanceManager.builder()
                .id(data.getId())
                .userId(data.getUserId())
                .lastSyncTimestamp(data.getLastSyncTimestamp())
                .lastVisitedRoute(data.getLastVisitedRoute())
                .sessionId(data.getSessionId())
                .dataImportTimestamp(data.getDataImportTimestamp())
                .presupuesto(presupuesto)
                .gastos(gastos)
                .categorias(categorias)
                .metasAhorro(metasAhorro)
                .recordatorios(recordatorios)
                .ingresosExtra(ingresosExtra)
                .eliminados(eliminadosEntity)
                .build();
    }

    /**
     * Convierte presupuesto
     */
    private Budget convertToBudget(BudgetData data, String userId) {
        if (data == null) return null;

        return Budget.builder()
                .id(data.getId())
                .userId(userId)
                .monto(data.getMonto())
                .fechaActualizacion(data.getFechaActualizacion())
                .build();
    }

    /**
     * Convierte gastos
     */
    private Map<String, Expenses> convertToExpensesMap(Map<String, ExpensesData> gastos, String userId) {
        Map<String, Expenses> result = new HashMap<>();

        if (gastos != null) {
            for (Map.Entry<String, ExpensesData> entry : gastos.entrySet()) {
                ExpensesData gastoData = entry.getValue();
                Expenses gasto = Expenses.builder()
                        .id(gastoData.getId())
                        .nombreG(gastoData.getNombreG())
                        .gasto(gastoData.getGasto())
                        .categoria(gastoData.getCategoria())
                        .fecha(gastoData.getFecha())
                        .userId(userId)
                        .origen(gastoData.getOrigen())
                        .recordatorioId(gastoData.getRecordatorioId())
                        .build();
                result.put(entry.getKey(), gasto);
            }
        }

        return result;
    }

    /**
     * Convierte categorías
     */
    private Map<String, Categories> convertToCategoriesMap(Map<String, CategoriesData> categorias, String userId) {
        Map<String, Categories> result = new HashMap<>();

        if (categorias != null) {
            for (Map.Entry<String, CategoriesData> entry : categorias.entrySet()) {
                CategoriesData categoriaData = entry.getValue();
                Categories categoria = Categories.builder()
                        .id(entry.getKey())
                        .nombre(categoriaData.getNombre())
                        .icono(categoriaData.getIcono())
                        .color(categoriaData.getColor())
                        .userId(userId)
                        .build();
                result.put(entry.getKey(), categoria);
            }
        }

        return result;
    }

    /**
     * Convierte metas de ahorro
     */
    private Map<String, SavingsGoal> convertToSavingsGoalMap(Map<String, SavingsGoalData> metas, String userId) {
        Map<String, SavingsGoal> result = new HashMap<>();

        if (metas != null) {
            for (Map.Entry<String, SavingsGoalData> entry : metas.entrySet()) {
                SavingsGoalData metaData = entry.getValue();
                SavingsGoal meta = SavingsGoal.builder()
                        .id(metaData.getId())
                        .nombre(metaData.getNombre())
                        .monto(metaData.getMonto())
                        .fechaObjetivo(metaData.getFechaObjetivo())
                        .descripcion(metaData.getDescripcion())
                        .creada(metaData.getCreada())
                        .ahorroAcumulado(metaData.getAhorroAcumulado())
                        .ahorroSemanal(metaData.getAhorroSemanal())
                        .ahorroMensual(metaData.getAhorroMensual())
                        .ahorroAnual(metaData.getAhorroAnual())
                        .diasRestantes(metaData.getDiasRestantes())
                        .completada(metaData.isCompletada())
                        .userId(userId)
                        .build();
                result.put(entry.getKey(), meta);
            }
        }

        return result;
    }

    /**
     * Convierte recordatorios
     */
    private Map<String, Reminder> convertToReminderMap(Map<String, ReminderData> recordatorios, String userId) {
        Map<String, Reminder> result = new HashMap<>();

        if (recordatorios != null) {
            for (Map.Entry<String, ReminderData> entry : recordatorios.entrySet()) {
                ReminderData reminderData = entry.getValue();
                Reminder reminder = Reminder.builder()
                        .id(reminderData.getId())
                        .titulo(reminderData.getTitulo())
                        .descripcion(reminderData.getDescripcion())
                        .monto(reminderData.getMonto())
                        .fechaVencimiento(reminderData.getFechaVencimiento())
                        .categoria(reminderData.getCategoria())
                        .esRecurrente(reminderData.isEsRecurrente())
                        .frecuencia(reminderData.getFrecuencia())
                        .diasAnticipacion(reminderData.getDiasAnticipacion())
                        .fechaCreacion(reminderData.getFechaCreacion())
                        .estado(reminderData.getEstado())
                        .userId(userId)
                        .build();
                result.put(entry.getKey(), reminder);
            }
        }

        return result;
    }

    /**
     * Convierte ingresos extra
     */
    private Map<String, ExtraIncome> convertToExtraIncomeMap(Map<String, ExtraIncomeData> ingresos, String userId) {
        Map<String, ExtraIncome> result = new HashMap<>();

        if (ingresos != null) {
            for (Map.Entry<String, ExtraIncomeData> entry : ingresos.entrySet()) {
                ExtraIncomeData incomeData = entry.getValue();
                ExtraIncome income = ExtraIncome.builder()
                        .id(incomeData.getId())
                        .monto(incomeData.getMonto())
                        .descripcion(incomeData.getDescripcion())
                        .fecha(incomeData.getFecha())
                        .userId(userId)
                        .build();
                result.put(entry.getKey(), income);
            }
        }

        return result;
    }

    /**
     * Convierte elementos eliminados
     */
    private FinanceManager.EliminadosInfo convertToEliminadosInfo(FinanceManagerData.EliminadosInfo eliminados) {
        if (eliminados == null) return null;

        return FinanceManager.EliminadosInfo.builder()
                .gastos(eliminados.getGastos())
                .categorias(eliminados.getCategorias())
                .metasAhorro(eliminados.getMetasAhorro())
                .recordatorios(eliminados.getRecordatorios())
                .ingresosExtra(eliminados.getIngresosExtra())
                .build();
    }
}