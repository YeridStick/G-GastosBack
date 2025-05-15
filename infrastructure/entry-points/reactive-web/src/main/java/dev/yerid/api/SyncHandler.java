package dev.yerid.api;

import dev.yerid.api.config.RateLimiter;
import dev.yerid.model.common.dto.SyncRequestDTO;
import dev.yerid.model.common.dto.SyncResponseDTO;
import dev.yerid.usecase.sync.SyncUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Manejador de endpoints para sincronización de datos entre cliente y servidor
 */
@Component
public class SyncHandler {
    private static final Logger logger = Logger.getLogger(SyncHandler.class.getName());
    private final SyncUseCase syncUseCase;
    private final RateLimiter rateLimiter;

    public SyncHandler(SyncUseCase syncUseCase, RateLimiter rateLimiter) {
        this.syncUseCase = syncUseCase;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Endpoint para recibir datos desde el cliente (sincronización ascendente)
     */
    public Mono<ServerResponse> uploadData(ServerRequest request) {
        logger.info("Recibida solicitud para sincronizar datos");

        // Obtener el token del encabezado de autorización
        String authToken = request.headers().firstHeader("Authorization");
        if (authToken != null && authToken.startsWith("Bearer ")) {
            authToken = authToken.substring(7);
        }

        final String sessionToken = authToken;

        return request.bodyToMono(SyncRequestDTO.class)
                .doOnNext(dto -> logger.info("Recibida petición de sincronización de usuario: " + dto.getEmail() +
                        ", datos: " + (dto.getData() != null ? dto.getData().size() : 0) + " elementos, " +
                        "eliminados: " + (dto.getEliminados() != null ? dto.getEliminados().size() : 0) + " elementos"))
                .flatMap(dto -> {
                    // Extrae datos de sincronización
                    String email = dto.getEmail();

                    // Verificar límite de tasa para este usuario
                    return rateLimiter.isRateLimited(email)
                            .flatMap(isLimited -> {
                                if (isLimited) {
                                    logger.warning("Límite de tasa excedido para usuario: " + email);
                                    return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .bodyValue(Map.of(
                                                    "error", "Demasiadas solicitudes",
                                                    "message", "Por favor, espere un momento antes de intentar de nuevo."
                                            ));
                                }

                                // Verificar si es la sesión activa
                                if (sessionToken != null && !rateLimiter.isActiveSession(email, sessionToken)) {
                                    // Registrar esta como la nueva sesión activa
                                    rateLimiter.registerSession(email, sessionToken);
                                    logger.info("Nueva sesión activa para usuario: " + email);
                                }

                                Map<String, Object> data = dto.getData();
                                Map<String, Object> eliminados = dto.getEliminados();
                                long timestamp = dto.getTimestamp();

                                logger.info("Procesando datos para: " + email + " con timestamp: " + timestamp);

                                // Usando el caso de uso refactorizado para procesar todos los datos en una operación
                                return syncUseCase.processSyncData(email, data, eliminados, timestamp)
                                        .then(Mono.just(Map.of(
                                                "status", "success",
                                                "timestamp", timestamp,
                                                "sessionActive", true
                                        )))
                                        .flatMap(response -> ServerResponse.ok()
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .bodyValue(response))
                                        .doOnSuccess(resp -> logger.info("Datos sincronizados correctamente para: " + email));
                            });
                })
                .onErrorResume(error -> {
                    logger.severe("Error al procesar sincronización: " + error.getMessage());
                    return ServerResponse.badRequest()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of(
                                    "error", "Error al procesar sincronización",
                                    "message", error.getMessage()
                            ));
                });
    }

    /**
     * Endpoint para enviar datos al cliente (sincronización descendente)
     */
    public Mono<ServerResponse> downloadData(ServerRequest request) {
        logger.info("Recibida solicitud para descargar datos");

        // Obtener el token del encabezado de autorización
        String authToken = request.headers().firstHeader("Authorization");
        if (authToken != null && authToken.startsWith("Bearer ")) {
            authToken = authToken.substring(7);
        }

        final String sessionToken = authToken;

        // Obtener parámetros de la consulta
        String userId = request.queryParam("userId").orElse("");
        long since = request.queryParam("since").map(Long::parseLong).orElse(0L);

        if (userId.isEmpty()) {
            return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("error", "El userId es requerido"));
        }

        logger.info("Descargando datos para usuario: " + userId + " desde timestamp: " + since);

        // Verificar límite de tasa para este usuario
        return rateLimiter.isRateLimited(userId)
                .flatMap(isLimited -> {
                    if (isLimited) {
                        logger.warning("Límite de tasa excedido para usuario: " + userId);
                        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of(
                                        "error", "Demasiadas solicitudes",
                                        "message", "Por favor, espere un momento antes de intentar de nuevo."
                                ));
                    }

                    // Verificar si es la sesión activa
                    if (sessionToken != null && !rateLimiter.isActiveSession(userId, sessionToken)) {
                        // Registrar esta como la nueva sesión activa
                        rateLimiter.registerSession(userId, sessionToken);
                        logger.info("Nueva sesión activa para usuario: " + userId);
                    }

                    // Usando el caso de uso refactorizado para obtener todos los datos en una operación
                    return syncUseCase.getUserData(userId, since)
                            .map(data -> {
                                SyncResponseDTO response = new SyncResponseDTO();
                                response.setData(data);
                                response.setTimestamp(System.currentTimeMillis());
                                response.setSessionActive(true);

                                // Log con información sobre la cantidad de datos enviados
                                if (data != null) {
                                    int totalItems = 0;
                                    if (data.containsKey("ObjetosGastos") && data.get("ObjetosGastos") instanceof java.util.List) {
                                        totalItems += ((java.util.List<?>) data.get("ObjetosGastos")).size();
                                    }
                                    if (data.containsKey("categorias") && data.get("categorias") instanceof java.util.List) {
                                        totalItems += ((java.util.List<?>) data.get("categorias")).size();
                                    }
                                    if (data.containsKey("MetasAhorro") && data.get("MetasAhorro") instanceof java.util.List) {
                                        totalItems += ((java.util.List<?>) data.get("MetasAhorro")).size();
                                    }
                                    if (data.containsKey("recordatorios") && data.get("recordatorios") instanceof java.util.List) {
                                        totalItems += ((java.util.List<?>) data.get("recordatorios")).size();
                                    }
                                    if (data.containsKey("IngresosExtra") && data.get("IngresosExtra") instanceof java.util.List) {
                                        totalItems += ((java.util.List<?>) data.get("IngresosExtra")).size();
                                    }

                                    logger.info("Enviando " + totalItems + " elementos en total al cliente para el usuario: " + userId);
                                }

                                return response;
                            })
                            .flatMap(response -> ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(response))
                            .doOnSuccess(resp -> logger.info("Datos enviados correctamente a cliente para: " + userId));
                })
                .onErrorResume(error -> {
                    logger.severe("Error al descargar datos: " + error.getMessage() + " - " + error.getClass().getName());
                    return ServerResponse.badRequest()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of(
                                    "error", "Error al descargar datos",
                                    "message", error.getMessage()
                            ));
                });
    }

    /**
     * Endpoint para cerrar sesión explícitamente
     */
    public Mono<ServerResponse> closeSession(ServerRequest request) {
        logger.info("Recibida solicitud para cerrar sesión");

        String authToken = request.headers().firstHeader("Authorization");
        if (authToken != null && authToken.startsWith("Bearer ")) {
            authToken = authToken.substring(7);
        }

        final String sessionToken = authToken;

        return request.bodyToMono(Map.class)
                .flatMap(body -> {
                    String userId = (String) body.get("email");

                    if (userId == null || userId.isEmpty()) {
                        return ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("error", "El email es requerido"));
                    }

                    logger.info("Cerrando sesión para usuario: " + userId);
                    rateLimiter.closeSession(userId, sessionToken);

                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of(
                                    "status", "success",
                                    "message", "Sesión cerrada correctamente"
                            ))
                            .doOnSuccess(resp -> logger.info("Sesión cerrada correctamente para: " + userId));
                });
    }
}