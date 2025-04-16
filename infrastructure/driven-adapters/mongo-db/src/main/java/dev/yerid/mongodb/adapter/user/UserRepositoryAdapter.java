package dev.yerid.mongodb.adapter.user;

import dev.yerid.model.user.User;
import dev.yerid.model.user.UserType;
import dev.yerid.model.user.gateways.UserRepository;
import dev.yerid.mongodb.data.user.UserData;
import dev.yerid.mongodb.repository.user.UserDataRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserDataRepository userDataRepository;

    public UserRepositoryAdapter(UserDataRepository userDataRepository) {
        this.userDataRepository = userDataRepository;
    }

    @Override
    public Mono<User> save(User user) {
        if (user.getId() == null || user.getId().isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            user = user.toBuilder()
                    .id(UUID.randomUUID().toString())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        } else {
            user = user.toBuilder()
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        UserData userData = toDocument(user);
        return userDataRepository.save(userData)
                .map(this::toEntity);
    }

    @Override
    public Mono<User> findById(String id) {
        return userDataRepository.findById(id)
                .map(this::toEntity);
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return userDataRepository.findByEmail(email)
                .map(this::toEntity);
    }

    @Override
    public Mono<User> saveVerificationCode(String email, String code, LocalDateTime expiry) {
        return findByEmail(email)
                .flatMap(user -> {
                    UserData userData = toDocument(user);
                    userData.setVerificationCode(code);
                    userData.setVerificationCodeExpiry(expiry);
                    return userDataRepository.save(userData).map(this::toEntity);
                });
    }

    @Override
    public Mono<User> verifyEmail(String code) {
        return userDataRepository.findByVerificationCodeAndVerificationCodeExpiryGreaterThan(code, LocalDateTime.now())
                .flatMap(userData -> {
                    userData.setEmailVerified(true);
                    userData.setVerificationCode(null);
                    userData.setVerificationCodeExpiry(null);
                    return userDataRepository.save(userData);
                })
                .map(this::toEntity);
    }

    @Override
    public Mono<User> updateLastLogin(String userId, String token, String ip) {
        return findById(userId)
                .flatMap(user -> {
                    UserData userData = toDocument(user);
                    userData.setLastToken(token);
                    userData.setLastIp(ip);
                    userData.setLastLoginAt(LocalDateTime.now());
                    return userDataRepository.save(userData);
                })
                .map(this::toEntity);
    }

    @Override
    public Mono<User> findByToken(String token) {
        return userDataRepository.findByLastToken(token)
                .map(this::toEntity);
    }

    // Métodos de conversión
    private User toEntity(UserData data) {
        if (data == null) return null;

        return User.builder()
                .id(data.getId())
                .email(data.getEmail())
                .name(data.getName())
                .userType(data.getUserType() != null ? UserType.valueOf(data.getUserType()) : null)
                .emailVerified(data.isEmailVerified())
                .createdAt(data.getCreatedAt())
                .updatedAt(data.getUpdatedAt())
                .lastLoginAt(data.getLastLoginAt())
                .build();
    }

    private UserData toDocument(User entity) {
        if (entity == null) return null;

        return UserData.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .name(entity.getName())
                .userType(entity.getUserType() != null ? entity.getUserType().name() : null)
                .emailVerified(entity.isEmailVerified())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .lastLoginAt(entity.getLastLoginAt())
                .build();
    }
}