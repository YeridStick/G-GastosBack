package dev.yerid.model.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String email;
    private String name;
    private UserType userType;
    private boolean emailVerified;

    // Estos campos son opcionales en el dominio pero útiles para el adaptador
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
}