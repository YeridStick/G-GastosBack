package dev.yerid.usecase.sync;

import dev.yerid.model.financeManager.finance.FinanceManager;
import dev.yerid.model.financeManager.finance.gateways.FinanceManagerRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Caso de uso para sincronización de datos entre el cliente y el servidor
 * Usa el repositorio FinanceManagerRepository para manejar todos los tipos de datos financieros
 */
@RequiredArgsConstructor
public class SyncUseCase {
    private static final Logger logger = Logger.getLogger(SyncUseCase.class.getName());
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final FinanceManagerRepository financeManagerRepository;

    /**
     * Procesa los datos de sincronización del cliente con soporte para elementos eliminados
     */
    public Mono<Void> processSyncData(String userId, Map<String, Object> data, Map<String, Object> eliminados, long timestamp) {
        logInfo("Iniciando sincronización de datos para usuario: " + userId + " con timestamp: " + timestamp);

        // Validar userId
        if (userId == null || userId.isEmpty()) {
            logError("Se intentó sincronizar datos con un userId vacío o nulo", null);
            return Mono.error(new IllegalArgumentException("El userId no puede estar vacío"));
        }

        // Usar el nuevo repositorio para actualizar todos los datos en un solo documento
        return financeManagerRepository.batchUpdate(userId, data, eliminados)
                .doOnSuccess(v -> logInfo("Sincronización completada para usuario: " + userId))
                .doOnError(e -> logError("Error en sincronización de datos para usuario: " + userId, e));
    }

    /**
     * Obtiene todos los datos del usuario para sincronización
     */
    public Mono<Map<String, Object>> getUserData(String userId, long since) {
        logInfo("Obteniendo datos para usuario: " + userId + " desde timestamp: " + since);

        // Obtener el documento completo y convertirlo al formato esperado por el cliente
        return financeManagerRepository.findByUserId(userId)
                .map(financeManager -> {
                    // Convertir a Map
                    return convertFinanceManagerToMap(financeManager);
                })
                .doOnSuccess(result -> logInfo("Datos obtenidos y preparados para usuario: " + userId))
                .doOnError(error -> logError("Error al obtener datos para usuario: " + userId, error));
    }

    /**
     * Convierte el objeto FinanceManager a un Map para enviarlo al cliente
     */
    private Map<String, Object> convertFinanceManagerToMap(FinanceManager financeManager) {
        Map<String, Object> result = new java.util.HashMap<>();

        // Presupuesto
        if (financeManager.getPresupuesto() != null) {
            result.put("PresupuestoLS", financeManager.getPresupuesto().getMonto());
        } else {
            result.put("PresupuestoLS", 0.0);
        }

        // Obtener listas de cada tipo de elemento
        result.put("ObjetosGastos", financeManager.getGastos() != null ?
                new java.util.ArrayList<>(financeManager.getGastos().values()) :
                new java.util.ArrayList<>());

        result.put("MetasAhorro", financeManager.getMetasAhorro() != null ?
                new java.util.ArrayList<>(financeManager.getMetasAhorro().values()) :
                new java.util.ArrayList<>());

        result.put("categorias", financeManager.getCategorias() != null ?
                new java.util.ArrayList<>(financeManager.getCategorias().values()) :
                new java.util.ArrayList<>());

        result.put("recordatorios", financeManager.getRecordatorios() != null ?
                new java.util.ArrayList<>(financeManager.getRecordatorios().values()) :
                new java.util.ArrayList<>());

        result.put("IngresosExtra", financeManager.getIngresosExtra() != null ?
                new java.util.ArrayList<>(financeManager.getIngresosExtra().values()) :
                new java.util.ArrayList<>());

        // Información adicional
        result.put("lastSyncTimestamp", financeManager.getLastSyncTimestamp());

        if (financeManager.getLastVisitedRoute() != null) {
            result.put("lastVisitedRoute", financeManager.getLastVisitedRoute());
        }

        if (financeManager.getSessionId() != null) {
            result.put("sessionId", financeManager.getSessionId());
        }

        if (financeManager.getDataImportTimestamp() != null) {
            result.put("dataImportTimestamp", financeManager.getDataImportTimestamp());
        }

        // Elementos eliminados
        if (financeManager.getEliminados() != null) {
            Map<String, Object> eliminados = new java.util.HashMap<>();
            eliminados.put("ObjetosGastos", financeManager.getEliminados().getGastos());
            eliminados.put("categorias", financeManager.getEliminados().getCategorias());
            eliminados.put("MetasAhorro", financeManager.getEliminados().getMetasAhorro());
            eliminados.put("recordatorios", financeManager.getEliminados().getRecordatorios());
            eliminados.put("IngresosExtra", financeManager.getEliminados().getIngresosExtra());

            result.put("eliminados", eliminados);
        }

        logDebug("Datos preparados: " + result.keySet());

        return result;
    }

    // Métodos de logging
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

    private String getTimestamp() {
        return "[" + LocalDateTime.now().format(formatter) + "]";
    }
}