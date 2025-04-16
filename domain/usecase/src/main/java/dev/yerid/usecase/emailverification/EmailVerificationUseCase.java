package dev.yerid.usecase.emailverification;

import dev.yerid.model.authenticationsession.gateways.AuthenticationSessionRepository;
import dev.yerid.model.verificationcode.VerificationCode;
import dev.yerid.model.verificationcode.gateways.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class EmailVerificationUseCase {
    private static final Logger log = Logger.getLogger(EmailVerificationUseCase.class.getName());

    private final VerificationCodeRepository emailRepository;
    private final AuthenticationSessionRepository authenticationRepository;
    private final SecureRandom random = new SecureRandom();

    public Mono<String> sendVerificationCode(String email) {
        log.info("Iniciando proceso de envío de código de verificación para: " + email);

        String code = generateVerificationCode();
        log.fine("Código generado: " + code);

        return authenticationRepository.storeCode(email, code)
                .doOnSuccess(v -> log.info("Código almacenado en la base de datos para: " + email))
                .doOnError(e -> log.log(Level.SEVERE, "Error al almacenar código para " + email + ": " + e.getMessage(), e))
                .then(Mono.just(VerificationCode.builder()
                        .email(email)
                        .code(code)
                        .expirationTime(LocalDateTime.now().plusMinutes(5))
                        .build()))
                .flatMap(verificationCode -> {
                    log.info("Enviando código de verificación por correo a: " + email);
                    return emailRepository
                            .sendVerificationCode(verificationCode)
                            .doOnSuccess(v -> log.info("Código enviado exitosamente a: " + email))
                            .doOnError(e -> log.log(Level.SEVERE, "Error al enviar código por correo a " + email + ": " + e.getMessage(), e))
                            .thenReturn(verificationCode.getCode());
                })
                .doOnSuccess(code1 -> log.info("Proceso de envío de código completado para: " + email))
                .doOnError(e -> log.log(Level.SEVERE, "Error en el proceso de envío de código para " + email + ": " + e.getMessage(), e));
    }

    private String generateVerificationCode() {
        return String.format("%06d", random.nextInt(1000000));
    }
}