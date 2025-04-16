package dev.yerid.usecase.sync;

import dev.yerid.model.financeManager.budget.Budget;
import dev.yerid.model.financeManager.budget.gateways.BudgetRepository;
import dev.yerid.model.financeManager.categories.Categories;
import dev.yerid.model.financeManager.categories.gateways.CategoriesRepository;
import dev.yerid.model.financeManager.expenses.Expenses;
import dev.yerid.model.financeManager.expenses.gateways.ExpensesRepository;
import dev.yerid.model.financeManager.extraincome.ExtraIncome;
import dev.yerid.model.financeManager.extraincome.gateways.ExtraIncomeRepository;
import dev.yerid.model.financeManager.reminder.Reminder;
import dev.yerid.model.financeManager.reminder.gateways.ReminderRepository;
import dev.yerid.model.financeManager.savingsgoal.SavingsGoal;
import dev.yerid.model.financeManager.savingsgoal.gateways.SavingsGoalRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class SyncUseCase {
    private static final Logger logger = Logger.getLogger(SyncUseCase.class.getName());
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final ExpensesRepository expensesRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final CategoriesRepository categoriesRepository;
    private final ReminderRepository reminderRepository;
    private final BudgetRepository budgetRepository;
    private final ExtraIncomeRepository extraIncomeRepository;


    public Mono<Expenses> saveExpeses(Expenses expenses) {
        return expensesRepository.save(expenses);
    }

    /**
     * Procesa los datos de sincronización desde el cliente con mejor manejo de errores y ejecución independiente
     */
    public Mono<Void> processSyncData(String userId, Map<String, Object> data, long timestamp) {
        logInfo("Iniciando sincronización de datos para usuario: " + userId + " con timestamp: " + timestamp);

        // Validar userId
        if (userId == null || userId.isEmpty()) {
            logError("Se intentó sincronizar datos con un userId vacío o nulo", null);
            return Mono.error(new IllegalArgumentException("El userId no puede estar vacío"));
        }

        // Verificar presencia de datos críticos y logearlos
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> gastos = (List<Map<String, Object>>) data.get("ObjetosGastos");
        logInfo("Gastos recibidos: " + (gastos != null ? gastos.size() : 0));

        if (gastos != null && !gastos.isEmpty()) {
            for (Map<String, Object> gasto : gastos) {
                logDebug("Gasto recibido - ID: " + gasto.get("id") +
                        ", Nombre: " + gasto.get("nombreG") +
                        ", Monto: " + gasto.get("gasto") +
                        ", Categoría: " + gasto.get("categoria"));
            }
        }

        // En lugar de usar Mono.zip que requiere que todos los procesos tengan éxito,
        // usaremos ejecución independiente para cada tipo de datos

        // Primero, procesamos cada tipo de datos independientemente
        Mono<Void> processGastos = processExpenses(userId, getListFromData(data, "ObjetosGastos"))
                .doOnSuccess(v -> logInfo("Procesamiento de gastos completado"))
                .doOnError(e -> logError("Error en procesamiento de gastos", e))
                .onErrorResume(e -> Mono.empty()); // Continuar aunque falle este proceso

        Mono<Void> processAhorro = processSavingsGoals(userId, getListFromData(data, "MetasAhorro"))
                .doOnSuccess(v -> logInfo("Procesamiento de metas completado"))
                .doOnError(e -> logError("Error en procesamiento de metas", e))
                .onErrorResume(e -> Mono.empty());

        Mono<Void> processCategorias = processCategories(userId, getListFromData(data, "categorias"))
                .doOnSuccess(v -> logInfo("Procesamiento de categorías completado"))
                .doOnError(e -> logError("Error en procesamiento de categorías", e))
                .onErrorResume(e -> Mono.empty());

        Mono<Void> processRecordatorios = processReminders(userId, getListFromData(data, "recordatorios"))
                .doOnSuccess(v -> logInfo("Procesamiento de recordatorios completado"))
                .doOnError(e -> logError("Error en procesamiento de recordatorios", e))
                .onErrorResume(e -> Mono.empty());

        Mono<Void> processPresupuesto = processBudget(userId, data.get("PresupuestoLS"))
                .doOnSuccess(v -> logInfo("Procesamiento de presupuesto completado"))
                .doOnError(e -> logError("Error en procesamiento de presupuesto", e))
                .onErrorResume(e -> Mono.empty());

        Mono<Void> processIngresosExtra = processExtraIncomes(userId, getListFromData(data, "IngresosExtra"))
                .doOnSuccess(v -> logInfo("Procesamiento de ingresos extra completado"))
                .doOnError(e -> logError("Error en procesamiento de ingresos extra", e))
                .onErrorResume(e -> Mono.empty());

        // Luego ejecutamos todos, pero permitiendo que continúen aunque alguno falle
        return Mono.whenDelayError(
                        processGastos,
                        processAhorro,
                        processCategorias,
                        processRecordatorios,
                        processPresupuesto,
                        processIngresosExtra
                )
                .doOnSuccess(v -> logInfo("Sincronización completada para usuario: " + userId))
                .doOnError(e -> logError("Error general en sincronización para usuario: " + userId, e));
    }

    /**
     * Obtiene todos los datos del usuario para sincronización con mejor debug
     */
    public Mono<Map<String, Object>> getUserData(String userId, long since) {
        logInfo("Obteniendo datos para usuario: " + userId + " desde timestamp: " + since);

        return Mono.zip(
                        expensesRepository.findByUserIdAndUpdatedSince(userId, since)
                                .doOnSubscribe(subscription -> logDebug("Iniciando búsqueda de gastos para usuario: " + userId))
                                .doOnNext(expense -> logDebug("Gasto encontrado: " + expense.getId() +
                                        ", Nombre: " + expense.getNombreG() +
                                        ", Usuario: " + expense.getUserId()))
                                .collectList()
                                .doOnSuccess(list -> logInfo("Gastos recuperados: " + list.size())),

                        // Otros elementos del zip...
                        savingsGoalRepository.findByUserIdAndUpdatedSince(userId, since)
                                .collectList(),

                        categoriesRepository.findByUserIdAndUpdatedSince(userId, since)
                                .collectList(),

                        reminderRepository.findByUserIdAndUpdatedSince(userId, since)
                                .collectList(),

                        budgetRepository.findByUserId(userId)
                                .defaultIfEmpty(Budget.builder().userId(userId).monto(0).build()),

                        extraIncomeRepository.findByUserIdAndUpdatedSince(userId, since)
                                .collectList()
                ).map(tuple -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("ObjetosGastos", tuple.getT1());
                    result.put("MetasAhorro", tuple.getT2());
                    result.put("categorias", tuple.getT3());
                    result.put("recordatorios", tuple.getT4());
                    result.put("PresupuestoLS", tuple.getT5().getMonto());
                    result.put("IngresosExtra", tuple.getT6());

                    logInfo("Datos consolidados listos para enviar al cliente");

                    // Agregar log detallado de los gastos que se envían al cliente
                    List<Expenses> gastos = tuple.getT1();
                    if (gastos.isEmpty()) {
                        logInfo("No se encontraron gastos para el usuario: " + userId);
                    } else {
                        logInfo("Enviando " + gastos.size() + " gastos al cliente");
                        for (Expenses gasto : gastos) {
                            logDebug("Gasto a enviar - ID: " + gasto.getId() +
                                    ", Nombre: " + gasto.getNombreG() +
                                    ", Monto: " + gasto.getGasto() +
                                    ", Usuario: " + gasto.getUserId());
                        }
                    }

                    return result;
                })
                .doOnError(error -> logError("Error al obtener datos para usuario: " + userId, error));
    }

    /**
     * Procesa los gastos recibidos en la sincronización con mejor manejo de errores y seguimiento
     */
    private Mono<Void> processExpenses(String userId, List<Map<String, Object>> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            logInfo("No hay gastos para procesar");
            return Mono.empty();
        }

        logInfo("Procesando " + expenses.size() + " gastos para el usuario: " + userId);

        // Para controlar mejor la ejecución, primero convertiremos todos los gastos
        // y luego los guardaremos uno por uno, manteniendo un registro de éxitos y fallos
        return Flux.fromIterable(expenses)
                .index() // Para tener un contador de posición
                .doOnSubscribe(s -> logInfo("Iniciando procesamiento de " + expenses.size() + " gastos"))
                // Convertir cada Map a un objeto Expenses
                .flatMap(tuple -> {
                    long index = tuple.getT1();
                    Map<String, Object> expenseMap = tuple.getT2();

                    try {
                        logDebug("Convirtiendo gasto #" + index + " de Map a Expenses");
                        Expenses expense = convertMapToExpense(expenseMap);

                        // Verificar que tenga ID
                        if (expense.getId() == null || expense.getId().isEmpty()) {
                            String newId = UUID.randomUUID().toString();
                            expense.setId(newId);
                            logInfo("Gasto #" + index + " sin ID, generado nuevo ID: " + newId);
                        }

                        // Forzar el userId correcto
                        String originalUserId = expense.getUserId();
                        expense.setUserId(userId);

                        if (originalUserId != null && !originalUserId.equals(userId)) {
                            logInfo("Gasto #" + index + " tenía userId diferente: " + originalUserId +
                                    ", actualizado a: " + userId);
                        }

                        logInfo("Gasto #" + index + " convertido: ID=" + expense.getId() +
                                ", Nombre=" + expense.getNombreG() +
                                ", Monto=" + expense.getGasto() +
                                ", Categoría=" + expense.getCategoria() +
                                ", Usuario=" + expense.getUserId());

                        return Mono.just(expense);
                    } catch (Exception e) {
                        logError("Error al convertir gasto #" + index, e);
                        return Mono.empty(); // Continuar con el siguiente
                    }
                })
                // Acumular todos los gastos convertidos
                .collectList()
                .doOnNext(convertedExpenses ->
                        logInfo("Conversión completada. Convertidos exitosamente " + convertedExpenses.size() +
                                " de " + expenses.size() + " gastos"))
                // Ahora guardar cada gasto convertido
                .flatMapMany(convertedExpenses -> Flux.fromIterable(convertedExpenses)
                        .index()
                        .flatMap(tuple -> {
                            long index = tuple.getT1();
                            Expenses expense = tuple.getT2();

                            logInfo("Guardando gasto #" + index + ", ID: " + expense.getId());

                            return expensesRepository.save(expense)
                                    .doOnSubscribe(s -> logDebug("Iniciando guardado en BD del gasto #" + index))
                                    .doOnSuccess(savedExpense ->
                                            logInfo("Gasto #" + index + " guardado exitosamente: " + savedExpense.getId() +
                                                    " para usuario: " + savedExpense.getUserId()))
                                    .doOnError(error ->
                                            logError("Error al guardar gasto #" + index + ", ID: " + expense.getId(), error))
                                    .onErrorResume(error -> {
                                        // Registrar el error pero continuar con otros gastos
                                        logError("Error recuperable al guardar gasto #" + index +
                                                ", ID: " + expense.getId() + ". Continuando con otros gastos.", error);
                                        return Mono.empty();
                                    });
                        })
                        // Contar cuántos se guardaron exitosamente
                        .count()
                        .doOnNext(count ->
                                logInfo("Proceso de guardado completado. Guardados exitosamente " + count +
                                        " de " + expenses.size() + " gastos para usuario: " + userId))
                )
                .then()
                .doOnSuccess(v -> logInfo("Procesamiento de gastos completado para usuario: " + userId))
                .doOnError(error -> logError("Error general en procesamiento de gastos para usuario: " + userId, error))
                .onErrorResume(e -> {
                    logError("Error recuperable en processExpenses", e);
                    return Mono.empty();
                });
    }

    /**
     * Procesa las metas de ahorro recibidas en la sincronización
     */
    private Mono<Void> processSavingsGoals(String userId, List<Map<String, Object>> goals) {
        if (goals == null || goals.isEmpty()) {
            logInfo("No hay metas de ahorro para procesar");
            return Mono.empty();
        }

        logInfo("Procesando " + goals.size() + " metas de ahorro para el usuario: " + userId);

        return Flux.fromIterable(goals)
                .doOnNext(goalMap -> logDebug("Procesando meta de ahorro con ID: " + goalMap.get("id")))
                .flatMap(goalMap -> {
                    SavingsGoal goal = convertMapToSavingsGoal(goalMap);
                    goal.setUserId(userId);
                    logDebug("Meta convertida: " + goal.getId() + ", Nombre: " + goal.getNombre() + ", Monto: " + goal.getMonto());

                    // Actualizamos la meta y también los gastos de ahorro relacionados
                    return savingsGoalRepository.save(goal)
                            .doOnNext(savedGoal -> logInfo("Meta de ahorro guardada: " + savedGoal.getId()))
                            .flatMap(savedGoal -> {
                                logDebug("Recalculando ahorro acumulado para meta: " + savedGoal.getNombre());
                                // Recalcular ahorro acumulado basado en gastos de ahorro
                                return expensesRepository.findByUserIdAndCategoria(userId, "Ahorro")
                                        .filter(expense -> expense.getNombreG() != null &&
                                                expense.getNombreG().contains(savedGoal.getNombre()))
                                        .doOnNext(expense -> logDebug("Gasto de ahorro encontrado: " + expense.getId() + " con monto: " + expense.getGasto()))
                                        .map(Expenses::getGasto)
                                        .reduce(0.0, Double::sum)
                                        .flatMap(totalSaved -> {
                                            logInfo("Ahorro total calculado para meta " + savedGoal.getNombre() + ": " + totalSaved);
                                            savedGoal.setAhorroAcumulado(totalSaved);
                                            savedGoal.setCompletada(totalSaved >= savedGoal.getMonto());
                                            logDebug("Estado de meta actualizada - Acumulado: " + totalSaved + ", Completada: " + savedGoal.isCompletada());
                                            return savingsGoalRepository.save(savedGoal);
                                        });
                            })
                            .then();
                })
                .doOnComplete(() -> logInfo("Procesamiento de metas de ahorro completado"))
                .doOnError(error -> logError("Error procesando metas de ahorro", error))
                .then();
    }

    /**
     * Procesa las categorías recibidas en la sincronización
     */
    private Mono<Void> processCategories(String userId, List<Map<String, Object>> categories) {
        if (categories == null || categories.isEmpty()) {
            logInfo("No hay categorías para procesar");
            return Mono.empty();
        }

        logInfo("Procesando " + categories.size() + " categorías para el usuario: " + userId);

        return Flux.fromIterable(categories)
                .doOnNext(categoryMap -> logDebug("Procesando categoría con ID: " + categoryMap.get("id")))
                .flatMap(categoryMap -> {
                    Categories category = convertMapToCategory(categoryMap);
                    category.setUserId(userId);
                    logDebug("Categoría convertida: " + category.getId() + ", Nombre: " + category.getNombre());

                    return categoriesRepository.save(category)
                            .doOnNext(savedCategory -> logInfo("Categoría guardada: " + savedCategory.getId()));
                })
                .doOnComplete(() -> logInfo("Procesamiento de categorías completado"))
                .doOnError(error -> logError("Error procesando categorías", error))
                .then();
    }

    /**
     * Procesa los recordatorios recibidos en la sincronización
     */
    private Mono<Void> processReminders(String userId, List<Map<String, Object>> reminders) {
        if (reminders == null || reminders.isEmpty()) {
            logInfo("No hay recordatorios para procesar");
            return Mono.empty();
        }

        logInfo("Procesando " + reminders.size() + " recordatorios para el usuario: " + userId);

        return Flux.fromIterable(reminders)
                .doOnNext(reminderMap -> logDebug("Procesando recordatorio con ID: " + reminderMap.get("id")))
                .flatMap(reminderMap -> {
                    Reminder reminder = convertMapToReminder(reminderMap);
                    reminder.setUserId(userId);
                    logDebug("Recordatorio convertido: " + reminder.getId() + ", Título: " + reminder.getTitulo() + ", Estado: " + reminder.getEstado());

                    // Si es un recordatorio completado, verificar que exista el gasto correspondiente
                    if ("completado".equals(reminder.getEstado())) {
                        logDebug("Recordatorio completado, verificando gasto asociado");
                        return expensesRepository.findByRecordatorioId(reminder.getId())
                                .hasElement()
                                .doOnNext(hasExpense -> logDebug("¿Existe gasto para recordatorio " + reminder.getId() + "?: " + hasExpense))
                                .flatMap(hasExpense -> {
                                    if (!hasExpense) {
                                        // Crear el gasto automáticamente si no existe
                                        logInfo("Creando gasto automático para recordatorio completado: " + reminder.getId());
                                        Expenses expense = Expenses.builder()
                                                .id(UUID.randomUUID().toString())
                                                .nombreG(reminder.getTitulo())
                                                .gasto(reminder.getMonto())
                                                .categoria(reminder.getCategoria())
                                                .fecha(reminder.getFechaCreacion())
                                                .userId(userId)
                                                .origen("recordatorio")
                                                .recordatorioId(reminder.getId())
                                                .build();
                                        return expensesRepository.save(expense)
                                                .doOnNext(savedExpense -> logInfo("Gasto automático creado: " + savedExpense.getId()))
                                                .then(reminderRepository.save(reminder));
                                    }
                                    logDebug("Gasto ya existe para recordatorio completado, solo actualizando recordatorio");
                                    return reminderRepository.save(reminder);
                                });
                    }

                    logDebug("Guardando recordatorio no completado");
                    return reminderRepository.save(reminder)
                            .doOnNext(savedReminder -> logInfo("Recordatorio guardado: " + savedReminder.getId()));
                })
                .doOnComplete(() -> logInfo("Procesamiento de recordatorios completado"))
                .doOnError(error -> logError("Error procesando recordatorios", error))
                .then();
    }

    /**
     * Procesa el presupuesto recibido en la sincronización
     */
    private Mono<Void> processBudget(String userId, Object budgetAmount) {
        if (budgetAmount == null) {
            logInfo("No hay presupuesto para procesar");
            return Mono.empty();
        }

        logInfo("Procesando presupuesto para el usuario: " + userId);
        double amount;

        try {
            if (budgetAmount instanceof Number) {
                amount = ((Number) budgetAmount).doubleValue();
            } else if (budgetAmount instanceof String) {
                amount = Double.parseDouble((String) budgetAmount);
            } else {
                logError("Formato de presupuesto inválido: " + budgetAmount.getClass().getName(), null);
                return Mono.error(new IllegalArgumentException("Invalid budget format"));
            }

            logDebug("Monto de presupuesto convertido: " + amount);
        } catch (Exception e) {
            logError("Error al convertir presupuesto", e);
            return Mono.error(new IllegalArgumentException("Error parsing budget amount", e));
        }

        return budgetRepository.findByUserId(userId)
                .doOnNext(existingBudget -> logDebug("Presupuesto existente encontrado con ID: " + existingBudget.getId()))
                .flatMap(budget -> {
                    logInfo("Actualizando presupuesto existente para usuario: " + userId);
                    budget.setMonto(amount);
                    budget.setFechaActualizacion(System.currentTimeMillis());
                    return budgetRepository.save(budget)
                            .doOnNext(savedBudget -> logInfo("Presupuesto actualizado: " + savedBudget.getId() + " con monto: " + savedBudget.getMonto()));
                })
                .switchIfEmpty(
                        Mono.defer(() -> {
                            logInfo("Creando nuevo presupuesto para usuario: " + userId);
                            String newId = UUID.randomUUID().toString();
                            logDebug("Nuevo ID de presupuesto generado: " + newId);

                            return budgetRepository.save(Budget.builder()
                                            .id(newId)
                                            .userId(userId)
                                            .monto(amount)
                                            .fechaActualizacion(System.currentTimeMillis())
                                            .build())
                                    .doOnNext(savedBudget -> logInfo("Nuevo presupuesto creado: " + savedBudget.getId() + " con monto: " + savedBudget.getMonto()));
                        })
                )
                .doOnError(error -> logError("Error procesando presupuesto", error))
                .then();
    }

    /**
     * Procesa los ingresos extra recibidos en la sincronización
     */
    private Mono<Void> processExtraIncomes(String userId, List<Map<String, Object>> incomes) {
        if (incomes == null || incomes.isEmpty()) {
            logInfo("No hay ingresos extra para procesar");
            return Mono.empty();
        }

        logInfo("Procesando " + incomes.size() + " ingresos extra para el usuario: " + userId);

        return Flux.fromIterable(incomes)
                .doOnNext(incomeMap -> logDebug("Procesando ingreso extra con ID: " + incomeMap.get("id")))
                .flatMap(incomeMap -> {
                    ExtraIncome income = convertMapToExtraIncome(incomeMap);
                    income.setUserId(userId);
                    logDebug("Ingreso extra convertido: " + income.getId() + ", Monto: " + income.getMonto() + ", Descripción: " + income.getDescripcion());

                    return extraIncomeRepository.save(income)
                            .doOnNext(savedIncome -> logInfo("Ingreso extra guardado: " + savedIncome.getId()));
                })
                .doOnComplete(() -> logInfo("Procesamiento de ingresos extra completado"))
                .doOnError(error -> logError("Error procesando ingresos extra", error))
                .then();
    }

    // Métodos de conversión de Map a entidades usando Java puro
    @SuppressWarnings("unchecked")
    private <T> List<Map<String, Object>> getListFromData(Map<String, Object> data, String key) {
        if (data != null && data.containsKey(key)) {
            List<Map<String, Object>> result = (List<Map<String, Object>>) data.get(key);
            logDebug("Extrayendo lista de " + key + ", encontrados: " + (result != null ? result.size() : 0) + " elementos");
            return result;
        }
        logDebug("Clave " + key + " no encontrada o vacía en los datos");
        return List.of();
    }

    // Los métodos de conversión quedan igual, los omito para brevedad

    private Expenses convertMapToExpense(Map<String, Object> map) {
        return Expenses.builder()
                .id(getStringValue(map, "id"))
                .nombreG(getStringValue(map, "nombreG"))
                .gasto(getDoubleValue(map, "gasto"))
                .categoria(getStringValue(map, "categoria"))
                .fecha(getLongValue(map, "fecha"))
                .origen(getStringValue(map, "origen"))
                .recordatorioId(getStringValue(map, "recordatorioId"))
                .build();
    }

    private SavingsGoal convertMapToSavingsGoal(Map<String, Object> map) {
        return SavingsGoal.builder()
                .id(getStringValue(map, "id"))
                .nombre(getStringValue(map, "nombre"))
                .monto(getDoubleValue(map, "monto"))
                .fechaObjetivo(getStringValue(map, "fechaObjetivo"))
                .descripcion(getStringValue(map, "descripcion"))
                .creada(getLongValue(map, "creada"))
                .ahorroAcumulado(getDoubleValue(map, "ahorroAcumulado"))
                .ahorroSemanal(getDoubleValue(map, "ahorroSemanal"))
                .ahorroMensual(getDoubleValue(map, "ahorroMensual"))
                .ahorroAnual(getDoubleValue(map, "ahorroAnual"))
                .diasRestantes(getIntValue(map, "diasRestantes"))
                .completada(getBooleanValue(map, "completada"))
                .build();
    }

    private Categories convertMapToCategory(Map<String, Object> map) {
        return Categories.builder()
                .id(getStringValue(map, "id"))
                .nombre(getStringValue(map, "nombre"))
                .icono(getStringValue(map, "icono"))
                .color(getStringValue(map, "color"))
                .build();
    }

    private Reminder convertMapToReminder(Map<String, Object> map) {
        return Reminder.builder()
                .id(getStringValue(map, "id"))
                .titulo(getStringValue(map, "titulo"))
                .descripcion(getStringValue(map, "descripcion"))
                .monto(getDoubleValue(map, "monto"))
                .fechaVencimiento(getLongValue(map, "fechaVencimiento"))
                .categoria(getStringValue(map, "categoria"))
                .esRecurrente(getBooleanValue(map, "esRecurrente"))
                .frecuencia(getStringValue(map, "frecuencia"))
                .diasAnticipacion(getIntValue(map, "diasAnticipacion"))
                .fechaCreacion(getLongValue(map, "fechaCreacion"))
                .estado(getStringValue(map, "estado"))
                .build();
    }

    private ExtraIncome convertMapToExtraIncome(Map<String, Object> map) {
        return ExtraIncome.builder()
                .id(getStringValue(map, "id"))
                .monto(getDoubleValue(map, "monto"))
                .descripcion(getStringValue(map, "descripcion"))
                .fecha(getLongValue(map, "fecha"))
                .build();
    }

    // Métodos auxiliares para extraer valores del mapa con seguridad
    private String getStringValue(Map<String, Object> map, String key) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            return map.get(key).toString();
        }
        return null;
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            } else if (value instanceof String) {
                try {
                    return Long.parseLong((String) value);
                } catch (NumberFormatException e) {
                    logDebug("Error al convertir a Long el valor para clave " + key + ": " + value);
                    return null;
                }
            }
        }
        return null;
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                try {
                    return Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    logDebug("Error al convertir a Double el valor para clave " + key + ": " + value);
                    return 0.0;
                }
            }
        }
        return 0.0;
    }

    private Integer getIntValue(Map<String, Object> map, String key) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    logDebug("Error al convertir a Integer el valor para clave " + key + ": " + value);
                    return 0;
                }
            }
        }
        return 0;
    }

    private Boolean getBooleanValue(Map<String, Object> map, String key) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            Object value = map.get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }
        }
        return false;
    }

    // Métodos de logging con Java puro
    private void logInfo(String message) {
        logger.info(getTimestamp() + " " + message);
    }

    private void logDebug(String message) {
        logger.fine(getTimestamp() + " " + message);
    }

    private void logError(String message, Throwable error) {
        if (error != null) {
            logger.log(Level.SEVERE, getTimestamp() + " " + message, error);
        } else {
            logger.severe(getTimestamp() + " " + message);
        }
    }

    private void logData(String prefix, Map<String, Object> data) {
        if (data == null) {
            logDebug(prefix + ": null");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append(":\n");

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                sb.append("  ").append(entry.getKey()).append(": [").append(list.size()).append(" elementos]\n");
            } else {
                sb.append("  ").append(entry.getKey()).append(": ").append(value).append("\n");
            }
        }

        logDebug(sb.toString());
    }

    private String getTimestamp() {
        return "[" + LocalDateTime.now().format(formatter) + "]";
    }
}