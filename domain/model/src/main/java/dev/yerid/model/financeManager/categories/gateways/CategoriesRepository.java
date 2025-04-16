package dev.yerid.model.financeManager.categories.gateways;

import dev.yerid.model.financeManager.categories.Categories;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CategoriesRepository {
    Mono<Categories> save(Categories categories);
    Mono<Categories> findById(String id);
    Flux<Categories> findByUserId(String userId);
    Mono<Void> deleteById(String id);
    Flux<Categories> findByUserIdAndUpdatedSince(String userId, long timestamp);
}
