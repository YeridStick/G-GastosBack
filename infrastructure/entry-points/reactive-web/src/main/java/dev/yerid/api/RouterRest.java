package dev.yerid.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {
    @Bean
    public RouterFunction<ServerResponse> routerFunction(Handler handler, SyncHandler syncHandler) {
        return route(GET("/api/usecase/path"), handler::listenGETUseCase)
                .andRoute(POST("/api/usecase/otherpath"), handler::listenPOSTUseCase)
                .andRoute(GET("/api/otherusercase/path"), handler::listenGETOtherUseCase)
                // Rutas de autenticación
                .andRoute(POST("/api/users/register"), handler::registerUser)
                .andRoute(POST("/api/auth/request-code"), handler::requestLoginCode)
                .andRoute(POST("/api/auth/verify-code"), handler::verifyCodeAndLogin)
                // Rutas de sincronización
                .andRoute(POST("/api/sync/upload"), syncHandler::uploadData)
                .andRoute(GET("/api/sync/download"), syncHandler::downloadData)
                .andRoute(POST("/api/sync/close-session"), syncHandler::closeSession);
    }
}