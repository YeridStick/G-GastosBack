package dev.yerid.model.verificationcode.gateways;

import dev.yerid.model.email.Notification;
import dev.yerid.model.verificationcode.VerificationCode;
import reactor.core.publisher.Mono;

public interface VerificationCodeRepository {
    Mono<Void> sendVerificationCode(VerificationCode verificationCode);
    Mono<Void> sendEmailNotification(Notification notification);
}
