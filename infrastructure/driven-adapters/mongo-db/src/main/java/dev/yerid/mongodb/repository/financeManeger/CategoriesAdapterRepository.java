package dev.yerid.mongodb.repository.financeManeger;

import dev.yerid.mongodb.data.financeManager.CategoriesData;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CategoriesAdapterRepository extends ReactiveMongoRepository<CategoriesData, String> {
    Flux<CategoriesData> findByUserId(String userId);
}