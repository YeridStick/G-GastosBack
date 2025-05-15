package dev.yerid.model.financeManager.finance.gateways;

import dev.yerid.model.financeManager.finance.FinanceManager;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface FinanceManagerRepository {
    Mono<FinanceManager> findByUserId(String userId);
    Mono<Void> batchUpdate(String userId, Map<String, Object> data, Map<String, Object> eliminados);
}
