package dev.yerid.mongodb.adapter.auth;

import dev.yerid.model.authenticationsession.gateways.AuthenticationSessionRepository;
import dev.yerid.mongodb.data.user.UserData;
import dev.yerid.mongodb.repository.auth.AuthenticationSessionDataRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthenticationSessionRepositoryAdapter implements AuthenticationSessionRepository {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationSessionRepositoryAdapter.class);
    private static final long TOKEN_EXPIRATION_TIME = 1000L * 60 * 60 * 24 * 365; // 1 año

    private final ReactiveMongoTemplate mongoTemplate;
    private final AuthenticationSessionDataRepository repository;

    private String jwtSecret;
    private Key key;

    public AuthenticationSessionRepositoryAdapter(
            ReactiveMongoTemplate mongoTemplate,
            AuthenticationSessionDataRepository repository) {
        this.mongoTemplate = mongoTemplate;
        this.repository = repository;
        // Inicializar con una clave temporal que será reemplazada cuando se establezca jwtSecret
        this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        log.info("AuthenticationSessionRepositoryAdapter inicializado con clave temporal");
    }

    // El setter de la propiedad que inicializa la clave cuando Spring lo invoca
    @Value("${jwt.secret}")
    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        log.info("JWT key inicializada con el secreto configurado");
    }

    @Override
    public Mono<Void> storeCode(String email, String code) {
        log.info("Almacenando código de verificación para el email: {}", email);

        // Primero intentamos encontrar un usuario existente
        return repository.findByEmail(email)
                .flatMap(existingUser -> {
                    // Si el usuario existe, solo actualizamos los campos del código
                    existingUser.setVerificationCode(code);
                    existingUser.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(5));
                    return repository.save(existingUser);
                })
                .switchIfEmpty(
                        // Si no existe, retornamos un error o una señal dependiendo de tu lógica
                        Mono.error(new RuntimeException("Usuario no encontrado: " + email))
                )
                .doOnSuccess(result -> log.info("Código almacenado exitosamente para: {}", email))
                .doOnError(error -> log.error("Error al almacenar código para {}: {}", email, error.getMessage()))
                .then();
    }

    @Override
    public Mono<String> extractUserIdFromToken(String token) {
        log.debug("Extrayendo userId del token JWT");

        try {
            // Parsear el token JWT para obtener los claims
            var claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Extraer el userId de los claims
            String userId = claims.get("userId", String.class);

            if (userId == null || userId.isEmpty()) {
                log.error("El token no contiene un userId válido");
                return Mono.error(new RuntimeException("Token inválido: no contiene userId"));
            }

            log.debug("UserId extraído del token: {}", userId);
            return Mono.just(userId);
        } catch (Exception e) {
            log.error("Error al extraer userId del token: {}", e.getMessage());
            return Mono.error(new RuntimeException("Error al procesar el token: " + e.getMessage()));
        }
    }

    @Override
    public Mono<String> getStoredCode(String email) {
        // Método existente sin cambios
        // ...
        return repository.findByEmailAndVerificationCodeExpiryGreaterThan(email, LocalDateTime.now())
                .doOnNext(userData -> log.debug("Usuario encontrado con código: {}", userData.getVerificationCode()))
                .doOnNext(userData -> {
                    LocalDateTime expiry = userData.getVerificationCodeExpiry();
                    log.debug("Tiempo de expiración: {}, Tiempo actual: {}, ¿Expirado?: {}",
                            expiry, LocalDateTime.now(), expiry.isBefore(LocalDateTime.now()));
                })
                .map(UserData::getVerificationCode)
                .doOnSuccess(code -> {
                    if (code != null) {
                        log.info("Código válido encontrado para: {}", email);
                    } else {
                        log.info("No se encontró código válido para: {}", email);
                    }
                })
                .doOnError(error -> log.error("Error al obtener código para {}: {}", email, error.getMessage()))
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("No se encontró un código válido para: {}", email);
                    return Mono.empty();
                }));
    }

    @Override
    public Mono<Void> invalidateCode(String email) {
        // Método existente sin cambios
        // ...
        return repository.findByEmail(email)
                .flatMap(userData -> {
                    userData.setVerificationCode(null);
                    userData.setVerificationCodeExpiry(null);
                    return repository.save(userData);
                })
                .doOnSuccess(result -> log.info("Código invalidado exitosamente para: {}", email))
                .doOnError(error -> log.error("Error al invalidar código para {}: {}", email, error.getMessage()))
                .then();
    }

    @Override
    public Mono<String> generateToken(String email, String ip) {
        log.info("Generando token JWT para el email: {}, IP: {}", email, ip);

        return repository.findByEmail(email)
                .doOnNext(userData -> log.debug("Usuario encontrado: {}, tipo: {}", userData.getEmail(), userData.getUserType()))
                .flatMap(userData -> {
                    // Generar token JWT con la información del usuario
                    Map<String, Object> claims = new HashMap<>();
                    claims.put("email", email);
                    claims.put("userId", userData.getId());

                    // Verificar si userType es null y usar un valor por defecto
                    String userTypeStr = "PERSONAL"; // Valor por defecto
                    if (userData.getUserType() != null) {
                        userTypeStr = userData.getUserType();
                    } else {
                        // Actualizar el documento con el tipo por defecto
                        userData.setUserType(userTypeStr);
                        return repository.save(userData).thenReturn(userTypeStr);
                    }

                    claims.put("userType", userTypeStr);

                    String token = Jwts.builder()
                            .setClaims(claims)
                            .setSubject(userData.getId())
                            .setIssuedAt(new Date())
                            .setExpiration(new Date(System.currentTimeMillis() + TOKEN_EXPIRATION_TIME)) // Usar la constante
                            .signWith(key)
                            .compact();

                    log.debug("Token JWT generado: {}", token);

                    // Actualizar información de sesión
                    userData.setLastToken(token);
                    userData.setLastIp(ip);
                    userData.setLastLoginAt(LocalDateTime.now());
                    userData.setEmailVerified(true);

                    return repository.save(userData)
                            .doOnSuccess(savedUser -> log.info("Información de sesión actualizada para: {}", email))
                            .doOnError(error -> log.error("Error al actualizar información de sesión para {}: {}", email, error.getMessage()))
                            .thenReturn(token);
                })
                .doOnSuccess(token -> log.info("Token generado exitosamente para: {}", email))
                .doOnError(error -> log.error("Error al generar token para {}: {}", email, error.getMessage()))
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("No se encontró el usuario con email: {}", email);
                    return Mono.error(new RuntimeException("Usuario no encontrado"));
                }));
    }

    @Override
    public Mono<Boolean> validateToken(String token, String ip) {
        log.info("Validando token JWT, IP: {}", ip);

        try {
            // Primero validar la firma y expiración del token
            var claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userId = claims.get("userId", String.class);
            log.info("Token válido criptográficamente para userId: {}", userId);

            // Opción más permisiva: no verificar en la base de datos
            return Mono.just(true);
        } catch (Exception e) {
            log.error("Error al validar token: {}", e.getMessage());
            return Mono.just(false);
        }
    }
}