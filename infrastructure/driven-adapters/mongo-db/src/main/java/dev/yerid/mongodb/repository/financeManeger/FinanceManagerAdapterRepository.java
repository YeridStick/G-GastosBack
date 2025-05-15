package dev.yerid.mongodb.repository.financeManeger;

import dev.yerid.mongodb.data.financeManager.FinanceManagerData;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface FinanceManagerAdapterRepository extends ReactiveMongoRepository<FinanceManagerData, String> {

    /**
     * Buscar los datos financieros por userId
     */
    Mono<FinanceManagerData> findByUserId(String userId);

    /**
     * Buscar los datos financieros actualizados desde una fecha
     */
    @Query("{ 'userId': ?0, 'lastSyncTimestamp': { $gt: ?1 } }")
    Mono<FinanceManagerData> findByUserIdAndUpdatedSince(String userId, long since);

    /**
     * Método para verificar si existe un documento para el usuario
     */
    Mono<Boolean> existsByUserId(String userId);
}