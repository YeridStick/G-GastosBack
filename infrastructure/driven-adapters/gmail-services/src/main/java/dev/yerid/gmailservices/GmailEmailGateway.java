package dev.yerid.gmailservices;

import dev.yerid.model.email.Notification;
import dev.yerid.model.verificationcode.VerificationCode;
import dev.yerid.model.verificationcode.gateways.VerificationCodeRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RequiredArgsConstructor
public class GmailEmailGateway implements VerificationCodeRepository {
    private final JavaMailSender mailSender;
    private final String corporateEmail;

    @Override
    public Mono<Void> sendVerificationCode(VerificationCode verificationCode) {
        return Mono.fromCallable(() -> {
                    MimeMessage message = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                    helper.setFrom(corporateEmail);
                    helper.setTo(verificationCode.getEmail());
                    helper.setSubject("Código de Autenticación en Dos Pasos");
                    helper.setText(getEmailTemplate(verificationCode.getCode()), true);

                    mailSender.send(message);
                    log.info("Correo de verificación enviado a: {}", verificationCode.getEmail());
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.error("Error al enviar email: {}", e.getMessage()))
                .then();
    }

    @Override
    public Mono<Void> sendEmailNotification(Notification notification) {
        return Mono.fromCallable(() -> {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(corporateEmail);
            helper.setTo(notification.getEmail());
            helper.setSubject(notification.getSubject());
            helper.setText(getGenericEmailTemplate(notification), true);

            mailSender.send(message);
            log.info("Notificación genérica enviada a: {}", notification.getEmail());
            return null;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(e -> log.error("Error al enviar email: {}", e.getMessage()))
        .then();
    }

    private String sanitizeInput(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private String getEmailTemplate(String code) {
        String sanitizedCode = sanitizeInput(code);

        return String.format("""
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <meta http-equiv="X-UA-Compatible" content="IE=edge">
        </head>
        <body style="font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif; background-color: #f4f4f5; margin: 0; padding: 0; line-height: 1.6;">
            <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color: #f4f4f5; padding: 20px 0;">
                <tr>
                    <td align="center" valign="top">
                        <table width="400" cellpadding="0" cellspacing="0" border="0" style="max-width: 400px; background-color: white; border-radius: 12px; box-shadow: 0 10px 30px rgba(0,0,0,0.08); padding: 30px; text-align: center; border: 1px solid #e4e4e7; margin: 0 auto;">
                            <tr>
                                <td align="center" style="padding: 0;">
                                    <div style="font-size: 48px; margin-bottom: 15px; display: block;">🔐</div>
                                    <h1 style="font-size: 20px; font-weight: 600; color: #18181b; margin: 0 0 10px 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">Código de Verificación</h1>
                                    <p style="font-size: 14px; color: #71717a; margin: 0 0 20px 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">Autenticación en Dos Pasos</p>
                                    
                                    <!-- Contenedor del código con botón de copiar -->
                                    <div style="position: relative; display: inline-block; width: 100%%;">
                                        <div id="verification-code" style="background-color: #f4f4f5; border-radius: 8px; padding: 15px; margin: 20px 0 10px 0; font-size: 18px; color: #18181b; letter-spacing: 4px; font-weight: 700; user-select: all; font-family: 'Courier New', monospace; position: relative;">
                                            %s
                                        </div>
                                        
                                        <!-- Botón de copiar -->
                                        <table cellpadding="0" cellspacing="0" border="0" style="margin: 0 auto 10px auto;">
                                            <tr>
                                                <td style="background-color: #10b981; border-radius: 6px;">
                                                    <a href="#" onclick="copyCode('%s'); return false;" style="display: inline-block; background-color: #10b981; color: white; text-decoration: none; padding: 8px 16px; border-radius: 6px; font-size: 12px; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; cursor: pointer;">
                                                        📋 Copiar código
                                                    </a>
                                                </td>
                                            </tr>
                                        </table>
                                    </div>
                                    
                                    <div style="background-color: #fff1f2; color: #881337; border-radius: 8px; padding: 15px; margin: 20px 0; font-size: 14px; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
                                        Este código expira en 5 minutos.<br>
                                        Nunca lo compartas con nadie.
                                    </div>
                                    
                                    <p style="font-size: 12px; color: #a1a1aa; margin: 20px 0 0 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
                                        Enviado por Gestión Financiera
                                    </p>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>

            <script>
                function copyCode(code) {
                    // Método moderno (Clipboard API)
                    if (navigator.clipboard && window.isSecureContext) {
                        navigator.clipboard.writeText(code).then(function() {
                            showCopyFeedback();
                        }).catch(function(err) {
                            fallbackCopyTextToClipboard(code);
                        });
                    } else {
                        // Fallback para navegadores antiguos
                        fallbackCopyTextToClipboard(code);
                    }
                }

                function fallbackCopyTextToClipboard(text) {
                    var textArea = document.createElement("textarea");
                    textArea.value = text;
                    textArea.style.top = "0";
                    textArea.style.left = "0";
                    textArea.style.position = "fixed";
                    textArea.style.opacity = "0";
                    
                    document.body.appendChild(textArea);
                    textArea.focus();
                    textArea.select();
                    
                    try {
                        var successful = document.execCommand('copy');
                        if (successful) {
                            showCopyFeedback();
                        }
                    } catch (err) {
                        console.error('Error al copiar: ', err);
                    }
                    
                    document.body.removeChild(textArea);
                }

                function showCopyFeedback() {
                    // Cambiar temporalmente el texto del botón
                    var buttons = document.querySelectorAll('a[onclick*="copyCode"]');
                    if (buttons.length > 0) {
                        var originalText = buttons[0].innerHTML;
                        buttons[0].innerHTML = '✅ ¡Copiado!';
                        buttons[0].style.backgroundColor = '#059669';
                        
                        setTimeout(function() {
                            buttons[0].innerHTML = originalText;
                            buttons[0].style.backgroundColor = '#10b981';
                        }, 2000);
                    }
                }
            </script>
        </body>
        </html>
        """, sanitizedCode, sanitizedCode);
    }
    
    private String getGenericEmailTemplate(Notification notification) {
        // Sanitize inputs to prevent potential HTML injection
        String userName = sanitizeInput(
                notification.getUserName() != null ? notification.getUserName() : "Usuario"
        );
        String message = sanitizeInput(
                notification.getMessage() != null ? notification.getMessage() : ""
        );
        String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm")
        );

        return String.format("""
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <meta http-equiv="X-UA-Compatible" content="IE=edge">
            </head>
            <body style="font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif; background-color: #f4f4f5; margin: 0; padding: 0; line-height: 1.6;">
                <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color: #f4f4f5; padding: 20px 0;">
                    <tr>
                        <td align="center" valign="top">
                            <table width="400" cellpadding="0" cellspacing="0" border="0" style="max-width: 400px; background-color: white; border-radius: 12px; box-shadow: 0 10px 30px rgba(0,0,0,0.08); padding: 30px; text-align: center; border: 1px solid #e4e4e7; margin: 0 auto;">
                                <tr>
                                    <td align="center" style="padding: 0;">
                                        <h1 style="font-size: 20px; font-weight: 600; color: #18181b; margin: 0 0 10px 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">Notificación</h1>
                                        <p style="font-size: 14px; color: #71717a; margin: 0 0 20px 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">Hola, %s</p>
                                        
                                        <div style="background-color: #f4f4f5; border-radius: 8px; padding: 15px; margin: 20px 0; font-size: 14px; color: #18181b; text-align: left; white-space: pre-line; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
                                            %s
                                        </div>
                                        
                                        <p style="font-size: 12px; color: #a1a1aa; margin: 0 0 20px 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">Enviado el %s</p>
                                        
                                        <table cellpadding="0" cellspacing="0" border="0" style="margin: 15px auto 0 auto;">
                                            <tr>
                                                <td style="background-color: #3b82f6; border-radius: 6px;">
                                                    <a href="http://localhost:3000/recordatorios" style="display: inline-block; background-color: #3b82f6; color: white; text-decoration: none; padding: 10px 20px; border-radius: 6px; font-size: 14px; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">Ver en la aplicación</a>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <p style="font-size: 12px; color: #a1a1aa; margin: 20px 0 0 0; font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
                                            Gestión Financiera
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """, userName, message, timestamp);
    }
}