package dev.yerid.api;

import dev.yerid.model.financeManager.expenses.Expenses;
import dev.yerid.usecase.sync.SyncUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.logging.Logger;

@Component
@RequiredArgsConstructor
public class TestHandler {
    private static final Logger logger = Logger.getLogger(TestHandler.class.getName());
    private final SyncUseCase syncUseCase;

    public Mono<ServerResponse> createTestExpense(ServerRequest request) {
        return request.bodyToMono(ExpenseTestRequest.class)
                .doOnNext(req -> logger.info("Recibida solicitud de prueba de gasto: " + req))
                .flatMap(req -> {
                    // Crear un gasto de prueba
                    String expenseId = UUID.randomUUID().toString();
                    logger.info("Creando gasto de prueba con ID: " + expenseId + " para usuario: " + req.userId());

                    Expenses expense = Expenses.builder()
                            .id(expenseId)
                            .nombreG(req.name())
                            .gasto(req.amount())
                            .categoria(req.category())
                            .fecha(System.currentTimeMillis())
                            .userId(req.userId())
                            .origen("test")
                            .build();

                    logger.info("Gasto construido: " + expense);

                    return syncUseCase.saveExpeses(expense)
                            .doOnSuccess(savedExpense ->
                                    logger.info("Gasto guardado exitosamente: " + savedExpense.getId() +
                                            " para usuario: " + savedExpense.getUserId()))
                            .doOnError(error ->
                                    logger.severe("Error al guardar gasto: " + error.getMessage()));
                })
                .flatMap(savedExpense ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(new ExpenseResponse(
                                        savedExpense.getId(),
                                        savedExpense.getNombreG(),
                                        savedExpense.getGasto(),
                                        savedExpense.getUserId(),
                                        "Gasto guardado correctamente")))
                .onErrorResume(e -> {
                    logger.severe("Error en endpoint: " + e.getMessage());
                    return ServerResponse.badRequest()
                            .bodyValue(new ErrorResponse("Error al guardar el gasto: " + e.getMessage()));
                });
    }

    public record ExpenseTestRequest(String userId, String name, double amount, String category) {}
    public record ExpenseResponse(String id, String name, double amount, String userId, String message) {}
    public record ErrorResponse(String message) {}
}