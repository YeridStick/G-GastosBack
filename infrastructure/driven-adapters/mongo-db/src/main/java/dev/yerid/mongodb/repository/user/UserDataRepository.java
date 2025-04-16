package dev.yerid.mongodb.repository.user;

import dev.yerid.mongodb.data.user.UserData;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface UserDataRepository extends ReactiveMongoRepository<UserData, String> {
    Mono<UserData> findByEmail(String email);
    Mono<UserData> findByVerificationCodeAndVerificationCodeExpiryGreaterThan(String code, LocalDateTime date);
    Mono<UserData> findByLastToken(String token);
}