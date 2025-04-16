package dev.yerid.model.financeManager.extraincome.gateways;

import dev.yerid.model.financeManager.extraincome.ExtraIncome;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ExtraIncomeRepository {
    Mono<ExtraIncome> save(ExtraIncome extraIncome);
    Mono<ExtraIncome> findById(String id);
    Flux<ExtraIncome> findByUserId(String userId);
    Mono<Void> deleteById(String id);
    Flux<ExtraIncome> findByUserIdAndUpdatedSince(String userId, long timestamp);
    Flux<ExtraIncome> findByUserIdAndDateRange(String userId, Long startDate, Long endDate);
    Mono<Double> getTotalExtraIncomeByUserId(String userId);
    Mono<Double> getCurrentMonthExtraIncomeByUserId(String userId);
}
