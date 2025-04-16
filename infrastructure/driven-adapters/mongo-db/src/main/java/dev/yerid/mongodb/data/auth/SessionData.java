package dev.yerid.mongodb.data.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionData {
    private String sessionId;
    private String deviceInfo;
    private String ipAddress;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}