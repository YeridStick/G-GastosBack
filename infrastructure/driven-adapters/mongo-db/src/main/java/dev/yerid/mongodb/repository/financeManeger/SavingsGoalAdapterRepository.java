package dev.yerid.mongodb.repository.financeManeger;

import dev.yerid.model.financeManager.savingsgoal.SavingsGoal;
import dev.yerid.mongodb.data.financeManager.SavingsGoalData;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface SavingsGoalAdapterRepository extends ReactiveMongoRepository<SavingsGoalData, String> {
    Flux<SavingsGoalData> findByUserId(String userId);
    Flux<SavingsGoalData> findByUserIdAndCompletada(String userId, Boolean completada);
    Flux<SavingsGoalData> findByUserIdAndCreadaGreaterThanEqual(String userId, Long timestamp);

}