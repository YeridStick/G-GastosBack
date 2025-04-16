package dev.yerid.model.authenticationsession.gateways;

import reactor.core.publisher.Mono;

public interface AuthenticationSessionRepository {
    Mono<Void> storeCode(String email, String code);
    Mono<String> getStoredCode(String email);
    Mono<Void> invalidateCode(String email);
    Mono<String> generateToken(String email, String ip);
    Mono<Boolean> validateToken(String token, String ip);
    Mono<String> extractUserIdFromToken(String token);
}