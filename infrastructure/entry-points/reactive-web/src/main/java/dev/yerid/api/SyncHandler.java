package dev.yerid.api;

import dev.yerid.model.user.gateways.UserRepository;
import dev.yerid.usecase.sync.SyncUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SyncHandler {
    private static final Logger log = LoggerFactory.getLogger(SyncHandler.class);
    private final SyncUseCase syncUseCase;
    private final UserRepository userRepository; // Inyecta tu repositorio de usuario existente

    public Mono<ServerResponse> uploadData(ServerRequest request) {
        return request.bodyToMono(SyncDataRequest.class)
                .flatMap(syncData -> {
                    String email = syncData.email();
                    log.info("Recibida solicitud de sincronización para usuario con email: {}", email);

                    // Usar tu adaptador existente para obtener el usuario por email
                    return userRepository.findByEmail(email)
                            .flatMap(user -> {
                                String userId = user.getId();
                                log.info("Usuario encontrado: {}, procesando sincronización", userId);

                                return syncUseCase.processSyncData(
                                                userId,
                                                syncData.data(),
                                                syncData.timestamp()
                                        )
                                        .then(ServerResponse.ok().build());
                            })
                            .switchIfEmpty(
                                    ServerResponse.badRequest()
                                            .bodyValue(new ErrorResponse("Usuario no encontrado con email: " + email))
                            );
                })
                .onErrorResume(e -> {
                    log.error("Error en sincronización: {}", e.getMessage());
                    return ServerResponse.badRequest()
                            .bodyValue(new ErrorResponse(e.getMessage()));
                });
    }

    public Mono<ServerResponse> downloadData(ServerRequest request) {
        String email = request.queryParam("userId")
                .orElseThrow(() -> new IllegalArgumentException("Email es requerido"));

        long since = request.queryParam("since")
                .map(Long::parseLong)
                .orElse(0L);

        log.info("Recibida solicitud de descarga para usuario con email: {}, desde: {}", email, since);

        // Usar tu adaptador existente para obtener el usuario por email
        return userRepository.findByEmail(email)
                .flatMap(user -> {
                    String userId = user.getId();
                    log.info("Usuario encontrado: {}, descargando datos", userId);

                    return syncUseCase.getUserData(userId, since)
                            .flatMap(data -> ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(data));
                })
                .switchIfEmpty(
                        ServerResponse.badRequest()
                                .bodyValue(new ErrorResponse("Usuario no encontrado con email: " + email))
                )
                .onErrorResume(e -> {
                    log.error("Error al obtener datos: {}", e.getMessage());
                    return ServerResponse.badRequest()
                            .bodyValue(new ErrorResponse(e.getMessage()));
                });
    }

    // Ahora recibimos el email en vez del userId
    public record SyncDataRequest(String email, Map<String, Object> data, long timestamp) {}
    public record ErrorResponse(String message) {}
}