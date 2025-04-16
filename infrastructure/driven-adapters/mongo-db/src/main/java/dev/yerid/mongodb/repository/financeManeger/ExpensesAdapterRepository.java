package dev.yerid.mongodb.repository.financeManeger;

import dev.yerid.mongodb.data.financeManager.ExpensesData;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ExpensesAdapterRepository extends ReactiveMongoRepository<ExpensesData, String> {
    Flux<ExpensesData> findByUserId(String userId);
    Flux<ExpensesData> findByUserIdAndCategoria(String userId, String categoria);
    Flux<ExpensesData> findByUserIdAndFechaGreaterThanEqual(String userId, Long timestamp);
    Mono<ExpensesData> findByRecordatorioId(String recordatorioId);
}