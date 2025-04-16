package dev.yerid.usecase.user;

import dev.yerid.model.user.User;
import dev.yerid.model.user.UserType;
import dev.yerid.model.user.gateways.UserRepository;
import dev.yerid.usecase.emailverification.EmailVerificationUseCase;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final EmailVerificationUseCase emailVerificationUseCase;

    public Mono<Object> registerUser(String email, String name, UserType userType) {
        return userRepository.findByEmail(email)
                .flatMap(existingUser -> Mono.error(new RuntimeException("Email ya registrado")))
                .switchIfEmpty(Mono.defer(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .name(name)
                            .userType(userType)
                            .emailVerified(false)
                            .build();

                    return userRepository.save(newUser);
                }));
    }
}