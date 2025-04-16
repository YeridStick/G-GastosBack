package dev.yerid.usecase.authenticate;

import dev.yerid.model.authenticationsession.gateways.AuthenticationSessionRepository;
import dev.yerid.model.user.gateways.UserRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class AuthenticateUseCase {
    private static final Logger log = Logger.getLogger(AuthenticateUseCase.class.getName());


    private final AuthenticationSessionRepository authenticationRepository;
    private final UserRepository userRepository;

    public Mono<String> authenticate(String email, String code, String ip) {
        // Verificar que el usuario existe y obtener su tipo
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")))
                .flatMap(user -> {
                    // Verificar el código de verificación
                    return authenticationRepository.getStoredCode(email)
                            .switchIfEmpty(Mono.error(new RuntimeException("Código expirado")))
                            .filter(savedCode -> savedCode.equals(code))
                            .switchIfEmpty(Mono.error(new RuntimeException("Código inválido")))
                            .flatMap(validCode -> authenticationRepository.invalidateCode(email)
                                    .then(authenticationRepository.generateToken(email, ip)));
                });
    }

    public Mono<Boolean> validateSession(String token, String ip) {
        return authenticationRepository.validateToken(token, ip);
    }

    /**
     * Extrae el ID de usuario de un token JWT.
     *
     * @param token El token JWT del cual extraer el ID de usuario
     * @return Un Mono con el ID de usuario o un error si el token es inválido
     */
    public Mono<String> getUserIdFromToken(String token) {
        log.log(Level.FINER,"Obteniendo userId del token: {}", token);
        return authenticationRepository.extractUserIdFromToken(token)
                .doOnSuccess(userId -> log.log( Level.SEVERE,"UserId obtenido del token: {}", userId))
                .doOnError(e -> log.log(Level.FINER,"Error al obtener userId del token: {}", e.getMessage()));
    }
}