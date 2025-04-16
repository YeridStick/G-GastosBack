package dev.yerid.mongodb.repository.auth;

import dev.yerid.mongodb.data.user.UserData;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface AuthenticationSessionDataRepository extends ReactiveMongoRepository<UserData, String> {

    Mono<UserData> findByEmail(String email);

    Mono<UserData> findByEmailAndVerificationCodeExpiryGreaterThan(String email, LocalDateTime now);


    Mono<UserData> findByLastToken(String token);
}