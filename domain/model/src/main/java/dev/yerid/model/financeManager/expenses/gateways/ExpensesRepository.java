package dev.yerid.model.financeManager.expenses.gateways;

import dev.yerid.model.financeManager.expenses.Expenses;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ExpensesRepository {
    Mono<Expenses> save(Expenses expenses);
    Mono<Expenses> findById(String id);
    Flux<Expenses> findByUserId(String userId);
    Flux<Expenses> findByUserIdAndCategoria(String userId, String categoria);
    Mono<Void> deleteById(String id);
    Flux<Expenses> findByUserIdAndUpdatedSince(String userId, long timestamp);
    Mono<Expenses> findByRecordatorioId(String recordatorioId);

}
