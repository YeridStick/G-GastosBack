package dev.yerid.model.financeManager.budget.gateways;

import dev.yerid.model.financeManager.budget.Budget;
import reactor.core.publisher.Mono;

public interface BudgetRepository {
    Mono<Budget> save(Budget budget);
    Mono<Budget> findById(String id);
    Mono<Budget> findByUserId(String userId);
    Mono<Void> deleteById(String id);
    Mono<Budget> updateBudgetAmount(String userId, double newAmount);
    Mono<Budget> increaseBudget(String userId, double amount);
    Mono<Budget> decreaseBudget(String userId, double amount);
}
