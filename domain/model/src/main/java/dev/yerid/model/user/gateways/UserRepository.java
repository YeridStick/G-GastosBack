package dev.yerid.model.user.gateways;

import dev.yerid.model.user.User;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface UserRepository {
    Mono<User> save(User user);
    Mono<User> findById(String id);
    Mono<User> findByEmail(String email);
    Mono<User> saveVerificationCode(String email, String code, LocalDateTime expiry);
    Mono<User> verifyEmail(String code);
    Mono<User> updateLastLogin(String userId, String token, String ip);
    Mono<User> findByToken(String token);
}