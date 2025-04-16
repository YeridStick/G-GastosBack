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
public class Session {
    private String sessionId;
    private String deviceInfo;
    private String ipAddress;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}