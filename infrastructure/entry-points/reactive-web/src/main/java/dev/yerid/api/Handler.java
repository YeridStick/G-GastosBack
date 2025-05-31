package dev.yerid.api;

import dev.yerid.model.email.Notification;
import dev.yerid.model.user.UserType;
import dev.yerid.usecase.authenticate.AuthenticateUseCase;
import dev.yerid.usecase.emailverification.EmailVerificationUseCase;
import dev.yerid.usecase.user.RegisterUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class Handler {

    private final RegisterUserUseCase registerUserUseCase;
    private final EmailVerificationUseCase emailVerificationUseCase;
    private final AuthenticateUseCase authenticateUseCase;

    public Mono<ServerResponse> listenGETUseCase(ServerRequest serverRequest) {
        return ServerResponse.ok().bodyValue("");
    }

    public Mono<ServerResponse> listenPOSTUseCase(ServerRequest serverRequest) {
        return ServerResponse.ok().bodyValue("");
    }

    public Mono<ServerResponse> listenGETOtherUseCase(ServerRequest serverRequest) {
        return ServerResponse.ok().bodyValue("");
    }

    public Mono<ServerResponse> registerUser(ServerRequest request) {
        return request.bodyToMono(UserRegistrationRequest.class)
                .flatMap(userRequest ->
                        registerUserUseCase.registerUser(
                                userRequest.getEmail(),
                                userRequest.getName(),
                                userRequest.getUserType()
                        )
                )
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .onErrorResume(e -> ServerResponse.badRequest()
                        .bodyValue(new ErrorResponse(e.getMessage())));
    }

    public Mono<ServerResponse> requestLoginCode(ServerRequest request) {
        return request.bodyToMono(LoginRequest.class)
                .flatMap(loginRequest ->
                        emailVerificationUseCase.sendVerificationCode(loginRequest.getEmail())
                                .then(ServerResponse.ok()
                                        .bodyValue(new MessageResponse("Código de verificación enviado")))
                                .onErrorResume(e -> {
                                    if (e.getMessage().contains("Usuario no encontrado")) {
                                        return ServerResponse.badRequest()
                                                .bodyValue(new ErrorResponse("Email no registrado"));
                                    }
                                    return ServerResponse.badRequest()
                                            .bodyValue(new ErrorResponse(e.getMessage()));
                                })
                );
    }

    public Mono<ServerResponse> sentNotification(ServerRequest request) {
        return  request.bodyToMono(Notification.class)
                .flatMap(notification ->
                        emailVerificationUseCase.sendNotification(notification)
                                .then(ServerResponse.ok()
                                        .bodyValue(new NotificationResponse(
                                                true,
                                                "Notificación enviada correctamente",
                                                "SUCCESS",
                                                notification.getEmail()
                                        )))
                                .onErrorResume(e -> ServerResponse.badRequest()
                                        .bodyValue(new NotificationResponse(
                                                false,
                                                "Error al enviar notificación: " + e.getMessage(),
                                                null,
                                                notification.getEmail()
                                        )))
                );
    }


    public Mono<ServerResponse> verifyCodeAndLogin(ServerRequest request) {
        return request.bodyToMono(VerificationRequest.class)
                .flatMap(verificationRequest -> {
                    String ip = request.remoteAddress()
                            .map(address -> address.getAddress().getHostAddress())
                            .orElse("unknown");

                    return authenticateUseCase.authenticate(
                                    verificationRequest.getEmail(),
                                    verificationRequest.getCode(),
                                    ip
                            )
                            .flatMap(token -> ServerResponse.ok()
                                    .bodyValue(new TokenResponse(token)))
                            .onErrorResume(e -> ServerResponse.badRequest()
                                    .bodyValue(new ErrorResponse(e.getMessage())));
                });
    }

    record VerificationRequest(String email, String code) {
        public String getEmail() { return email; }
        public String getCode() { return code; }
    }

    record TokenResponse(String token) {}

    record LoginRequest(String email) {
        public String getEmail() { return email; }
    }

    record MessageResponse(String message) {}

    record UserRegistrationRequest(String email, String name, UserType userType) {
        public String getEmail() { return email; }
        public String getName() { return name; }
        public UserType getUserType() { return userType; }
    }

    record ErrorResponse(String message) {}

    record NotificationResponse(
            boolean success,
            String message,
            String result,
            String emailSentTo
    ) {}
}