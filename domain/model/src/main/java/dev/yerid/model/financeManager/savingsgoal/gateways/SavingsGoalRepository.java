package dev.yerid.model.financeManager.savingsgoal.gateways;

import dev.yerid.model.financeManager.savingsgoal.SavingsGoal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SavingsGoalRepository {
    Mono<SavingsGoal> save(SavingsGoal savingsGoal);
    Mono<SavingsGoal> findById(String id);
    Flux<SavingsGoal> findByUserId(String userId);
    Mono<Void> deleteById(String id);
    Flux<SavingsGoal> findCompletedGoalsByUserId(String userId);
    Flux<SavingsGoal> findPendingGoalsByUserId(String userId);
    Mono<SavingsGoal> updateSavedAmount(String id, double newAmount);
    Flux<SavingsGoal> findByUserIdAndUpdatedSince(String userId, long timestamp);
}
