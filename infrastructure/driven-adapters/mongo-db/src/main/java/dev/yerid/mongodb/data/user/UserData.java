package dev.yerid.mongodb.data.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class UserData {
    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String name;
    private String userType; // PERSONAL, ENTREPRENEUR, BUSINESS

    // Para verificación y autenticación
    private boolean emailVerified;
    private String verificationCode;
    private LocalDateTime verificationCodeExpiry;

    // Para JWT y seguimiento de sesiones
    private String lastToken;
    private String lastIp;
    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}