package dev.yerid.mongodb.repository.financeManeger;

import dev.yerid.mongodb.data.financeManager.ExtraIncomeData;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ExtraIncomeAdapterRepository extends ReactiveMongoRepository<ExtraIncomeData, String> {
    Flux<ExtraIncomeData> findByUserId(String userId);
    Flux<ExtraIncomeData> findByUserIdAndFechaGreaterThanEqual(String userId, Long timestamp);
}