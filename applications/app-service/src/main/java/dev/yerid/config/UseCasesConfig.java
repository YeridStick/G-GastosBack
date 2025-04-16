package dev.yerid.config;

import dev.yerid.model.authenticationsession.gateways.AuthenticationSessionRepository;
import dev.yerid.model.user.gateways.UserRepository;
import dev.yerid.model.verificationcode.gateways.VerificationCodeRepository;
import dev.yerid.usecase.authenticate.AuthenticateUseCase;
import dev.yerid.usecase.emailverification.EmailVerificationUseCase;
import dev.yerid.usecase.user.RegisterUserUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import java.security.SecureRandom;

@Configuration
@ComponentScan(basePackages = "dev.yerid.usecase",
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+UseCase$")
        },
        useDefaultFilters = false)
public class UseCasesConfig {
        @Bean
        public EmailVerificationUseCase emailVerificationUseCase(
                VerificationCodeRepository emailRepository,
                AuthenticationSessionRepository authenticationRepository
        ) {
                return new EmailVerificationUseCase(emailRepository, authenticationRepository);
        }

        @Bean
        public RegisterUserUseCase registerUserUseCase(
                UserRepository userRepository,
                EmailVerificationUseCase emailVerificationUseCase
        ) {
                return new RegisterUserUseCase(userRepository, emailVerificationUseCase);
        }

        @Bean
        public AuthenticateUseCase authenticateUseCase(
                AuthenticationSessionRepository authenticationRepository,
                UserRepository userRepository
        ) {
                return new AuthenticateUseCase(authenticationRepository, userRepository);
        }

        @Bean
        public SecureRandom secureRandom() {
                return new SecureRandom();
        }
}