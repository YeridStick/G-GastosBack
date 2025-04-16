package dev.yerid.model.authenticationsession;
import lombok.*;
//import lombok.NoArgsConstructor;
import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AuthenticationSession {
    private String token;
    private String email;
    private String ip;
    private LocalDateTime expirationTime;
    private boolean isValid;
}
