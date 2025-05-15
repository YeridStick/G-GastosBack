package dev.yerid.mongodb.adapter.financeManegerAdapters;

import dev.yerid.mongodb.adapter.financeManegerAdapters.utils.DataUtils;
import dev.yerid.mongodb.data.financeManager.*;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Clase utilitaria para extraer datos de los mapas recibidos del frontend
 */
@Component
public class FinanceManagerDataExtractor {

    /**
     * Prepara un documento completo a partir de los datos
     */
    public FinanceManagerData prepareDocument(String userId, Map<String, Object> data, Map<String, Object> eliminados) {
        FinanceManagerData document = FinanceManagerData.createEmpty(userId);
        document.setLastSyncTimestamp(System.currentTimeMillis());

        // Presupuesto
        if (data.containsKey("PresupuestoLS")) {
            Object presupuestoObj = data.get("PresupuestoLS");
            double monto = DataUtils.parseToDouble(presupuestoObj, 0.0);
            document.setPresupuesto(new BudgetData(userId, monto, System.currentTimeMillis()));
        }

        // Campos de seguimiento
        if (data.containsKey("lastVisitedRoute")) {
            document.setLastVisitedRoute(DataUtils.toString(data.get("lastVisitedRoute")));
        }
        if (data.containsKey("sessionId")) {
            document.setSessionId(DataUtils.toString(data.get("sessionId")));
        }
        if (data.containsKey("dataImportTimestamp")) {
            document.setDataImportTimestamp(DataUtils.parseToLong(data.get("dataImportTimestamp"), null));
        }

        // Gastos
        if (data.containsKey("ObjetosGastos")) {
            document.setGastos(extractMapOfExpenses(data, "ObjetosGastos"));
        }

        // Categorías
        if (data.containsKey("categorias")) {
            document.setCategorias(extractMapOfCategories(data, "categorias"));
        }

        // Metas de ahorro
        if (data.containsKey("MetasAhorro")) {
            document.setMetasAhorro(extractMapOfSavingsGoals(data, "MetasAhorro"));
        }

        // Recordatorios
        if (data.containsKey("recordatorios")) {
            document.setRecordatorios(extractMapOfReminders(data, "recordatorios"));
        }

        // Ingresos extra
        if (data.containsKey("IngresosExtra")) {
            document.setIngresosExtra(extractMapOfExtraIncomes(data, "IngresosExtra"));
        }

        // Elementos eliminados
        if (eliminados != null && !eliminados.isEmpty()) {
            FinanceManagerData.EliminadosInfo eliminadosInfo = new FinanceManagerData.EliminadosInfo();

            eliminadosInfo.setGastos(extractStringList(eliminados, "ObjetosGastos"));
            eliminadosInfo.setCategorias(extractStringList(eliminados, "categorias"));
            eliminadosInfo.setMetasAhorro(extractStringList(eliminados, "MetasAhorro"));
            eliminadosInfo.setRecordatorios(extractStringList(eliminados, "recordatorios"));
            eliminadosInfo.setIngresosExtra(extractStringList(eliminados, "IngresosExtra"));

            document.setEliminados(eliminadosInfo);
        }

        return document;
    }

    /**
     * Prepara un objeto Update para actualización parcial
     */
    public Update prepareUpdate(Map<String, Object> data, Map<String, Object> eliminados) {
        Update update = new Update();
        update.set("lastSyncTimestamp", System.currentTimeMillis());

        // Presupuesto
        if (data.containsKey("PresupuestoLS")) {
            Object presupuestoObj = data.get("PresupuestoLS");
            double monto = DataUtils.parseToDouble(presupuestoObj, 0.0);
            update.set("presupuesto.monto", monto);
            update.set("presupuesto.fechaActualizacion", System.currentTimeMillis());
        }

        // Campos de seguimiento
        if (data.containsKey("lastVisitedRoute")) {
            update.set("lastVisitedRoute", DataUtils.toString(data.get("lastVisitedRoute")));
        }
        if (data.containsKey("sessionId")) {
            update.set("sessionId", DataUtils.toString(data.get("sessionId")));
        }
        if (data.containsKey("dataImportTimestamp")) {
            update.set("dataImportTimestamp", DataUtils.parseToLong(data.get("dataImportTimestamp"), null));
        }

        // Actualizar colecciones
        // Para cada colección: extraer del mapa y actualizar cada elemento individual

        // Gastos
        Map<String, ExpensesData> gastos = extractMapOfExpenses(data, "ObjetosGastos");
        for (Map.Entry<String, ExpensesData> entry : gastos.entrySet()) {
            update.set("gastos." + entry.getKey(), entry.getValue());
        }

        // Categorías
        Map<String, CategoriesData> categorias = extractMapOfCategories(data, "categorias");
        for (Map.Entry<String, CategoriesData> entry : categorias.entrySet()) {
            update.set("categorias." + entry.getKey(), entry.getValue());
        }

        // Metas de ahorro
        Map<String, SavingsGoalData> metas = extractMapOfSavingsGoals(data, "MetasAhorro");
        for (Map.Entry<String, SavingsGoalData> entry : metas.entrySet()) {
            update.set("metasAhorro." + entry.getKey(), entry.getValue());
        }

        // Recordatorios
        Map<String, ReminderData> recordatorios = extractMapOfReminders(data, "recordatorios");
        for (Map.Entry<String, ReminderData> entry : recordatorios.entrySet()) {
            update.set("recordatorios." + entry.getKey(), entry.getValue());
        }

        // Ingresos extra
        Map<String, ExtraIncomeData> ingresos = extractMapOfExtraIncomes(data, "IngresosExtra");
        for (Map.Entry<String, ExtraIncomeData> entry : ingresos.entrySet()) {
            update.set("ingresosExtra." + entry.getKey(), entry.getValue());
        }

        // Procesar elementos eliminados
        if (eliminados != null && !eliminados.isEmpty()) {
            // Gastos eliminados
            List<String> gastosEliminados = extractStringList(eliminados, "ObjetosGastos");
            if (!gastosEliminados.isEmpty()) {
                update.addToSet("eliminados.gastos").each(gastosEliminados.toArray());
                for (String id : gastosEliminados) {
                    update.unset("gastos." + id);
                }
            }

            // Categorías eliminadas
            List<String> categoriasEliminadas = extractStringList(eliminados, "categorias");
            if (!categoriasEliminadas.isEmpty()) {
                update.addToSet("eliminados.categorias").each(categoriasEliminadas.toArray());
                for (String id : categoriasEliminadas) {
                    update.unset("categorias." + id);
                }
            }

            // Metas eliminadas
            List<String> metasEliminadas = extractStringList(eliminados, "MetasAhorro");
            if (!metasEliminadas.isEmpty()) {
                update.addToSet("eliminados.metasAhorro").each(metasEliminadas.toArray());
                for (String id : metasEliminadas) {
                    update.unset("metasAhorro." + id);
                }
            }

            // Recordatorios eliminados
            List<String> recordatoriosEliminados = extractStringList(eliminados, "recordatorios");
            if (!recordatoriosEliminados.isEmpty()) {
                update.addToSet("eliminados.recordatorios").each(recordatoriosEliminados.toArray());
                for (String id : recordatoriosEliminados) {
                    update.unset("recordatorios." + id);
                }
            }

            // Ingresos extra eliminados
            List<String> ingresosEliminados = extractStringList(eliminados, "IngresosExtra");
            if (!ingresosEliminados.isEmpty()) {
                update.addToSet("eliminados.ingresosExtra").each(ingresosEliminados.toArray());
                for (String id : ingresosEliminados) {
                    update.unset("ingresosExtra." + id);
                }
            }
        }

        return update;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> extractList(Map<String, Object> data, String key) {
        if (data != null && data.containsKey(key) && data.get(key) instanceof List) {
            List<?> list = (List<?>) data.get(key);
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                return (List<Map<String, Object>>) list;
            }
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractStringList(Map<String, Object> data, String key) {
        if (data != null && data.containsKey(key) && data.get(key) instanceof List) {
            List<?> list = (List<?>) data.get(key);
            return list.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public Map<String, ExpensesData> extractMapOfExpenses(Map<String, Object> data, String key) {
        Map<String, ExpensesData> result = new HashMap<>();
        List<Map<String, Object>> list = extractList(data, key);

        for (Map<String, Object> item : list) {
            String id = DataUtils.toString(item.get("id"));
            if (id == null || id.isEmpty()) {
                id = UUID.randomUUID().toString();
            }

            ExpensesData expenseData = ExpensesData.builder()
                    .id(id)
                    .nombreG(DataUtils.toString(item.get("nombreG")))
                    .gasto(DataUtils.parseToDouble(item.get("gasto"), 0.0))
                    .categoria(DataUtils.toString(item.get("categoria")))
                    .fecha(DataUtils.parseFecha(item.get("fecha")))
                    .origen(DataUtils.toString(item.get("origen")))
                    .recordatorioId(DataUtils.toString(item.get("recordatorioId")))
                    .build();

            result.put(id, expenseData);
        }

        return result;
    }

    public Map<String, CategoriesData> extractMapOfCategories(Map<String, Object> data, String key) {
        Map<String, CategoriesData> result = new HashMap<>();
        List<Map<String, Object>> list = extractList(data, key);

        for (Map<String, Object> item : list) {
            String id = DataUtils.toString(item.get("id"));
            if (id == null || id.isEmpty()) {
                id = DataUtils.generateCategoryId(DataUtils.toString(item.get("nombre")));
            }

            CategoriesData categoryData = CategoriesData.builder()
                    .nombre(DataUtils.toString(item.get("nombre")))
                    .icono(DataUtils.toString(item.get("icono")))
                    .color(DataUtils.toString(item.get("color")))
                    .build();

            result.put(id, categoryData);
        }

        return result;
    }

    public Map<String, SavingsGoalData> extractMapOfSavingsGoals(Map<String, Object> data, String key) {
        Map<String, SavingsGoalData> result = new HashMap<>();
        List<Map<String, Object>> list = extractList(data, key);

        for (Map<String, Object> item : list) {
            String id = DataUtils.toString(item.get("id"));
            if (id == null || id.isEmpty()) {
                id = UUID.randomUUID().toString();
            }

            SavingsGoalData goalData = SavingsGoalData.builder()
                    .id(id)
                    .nombre(DataUtils.toString(item.get("nombre")))
                    .monto(DataUtils.parseToDouble(item.get("monto"), 0.0))
                    .fechaObjetivo(DataUtils.toString(item.get("fechaObjetivo")))
                    .descripcion(DataUtils.toString(item.get("descripcion")))
                    .creada(DataUtils.parseToLong(item.get("creada"), System.currentTimeMillis()))
                    .ahorroAcumulado(DataUtils.parseToDouble(item.get("ahorroAcumulado"), 0.0))
                    .ahorroSemanal(DataUtils.parseToDouble(item.get("ahorroSemanal"), 0.0))
                    .ahorroMensual(DataUtils.parseToDouble(item.get("ahorroMensual"), 0.0))
                    .ahorroAnual(DataUtils.parseToDouble(item.get("ahorroAnual"), 0.0))
                    .diasRestantes(DataUtils.parseToInt(item.get("diasRestantes"), 0))
                    .completada(DataUtils.parseToBoolean(item.get("completada"), false))
                    .build();

            result.put(id, goalData);
        }

        return result;
    }

    public Map<String, ReminderData> extractMapOfReminders(Map<String, Object> data, String key) {
        Map<String, ReminderData> result = new HashMap<>();
        List<Map<String, Object>> list = extractList(data, key);

        for (Map<String, Object> item : list) {
            String id = DataUtils.toString(item.get("id"));
            if (id == null || id.isEmpty()) {
                id = UUID.randomUUID().toString();
            }

            ReminderData reminderData = ReminderData.builder()
                    .id(id)
                    .titulo(DataUtils.toString(item.get("titulo")))
                    .descripcion(DataUtils.toString(item.get("descripcion")))
                    .monto(DataUtils.parseToDouble(item.get("monto"), 0.0))
                    .fechaVencimiento(DataUtils.parseToLong(item.get("fechaVencimiento"), null))
                    .categoria(DataUtils.toString(item.get("categoria")))
                    .esRecurrente(DataUtils.parseToBoolean(item.get("esRecurrente"), false))
                    .frecuencia(DataUtils.toString(item.get("frecuencia")))
                    .diasAnticipacion(DataUtils.parseToInt(item.get("diasAnticipacion"), 0))
                    .fechaCreacion(DataUtils.parseToLong(item.get("fechaCreacion"), System.currentTimeMillis()))
                    .estado(DataUtils.toString(item.get("estado")))
                    .build();

            result.put(id, reminderData);
        }

        return result;
    }

    public Map<String, ExtraIncomeData> extractMapOfExtraIncomes(Map<String, Object> data, String key) {
        Map<String, ExtraIncomeData> result = new HashMap<>();
        List<Map<String, Object>> list = extractList(data, key);

        for (Map<String, Object> item : list) {
            String id = DataUtils.toString(item.get("id"));
            if (id == null || id.isEmpty()) {
                id = UUID.randomUUID().toString();
            }

            ExtraIncomeData incomeData = ExtraIncomeData.builder()
                    .id(id)
                    .monto(DataUtils.parseToDouble(item.get("monto"), 0.0))
                    .descripcion(DataUtils.toString(item.get("descripcion")))
                    .fecha(DataUtils.parseToLong(item.get("fecha"), System.currentTimeMillis()))
                    .build();

            result.put(id, incomeData);
        }

        return result;
    }
}