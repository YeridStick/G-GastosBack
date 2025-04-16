package dev.yerid.mongodb.repository.financeManeger;

import dev.yerid.mongodb.data.financeManager.BudgetData;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface BudgetAdapterRepository extends ReactiveMongoRepository<BudgetData, String> {
    Mono<BudgetData> findByUserId(String userId);
}