package dev.yerid.mongodb.repository.financeManeger;

import dev.yerid.mongodb.data.financeManager.ReminderData;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ReminderAdapterRepository extends ReactiveMongoRepository<ReminderData, String> {
    Flux<ReminderData> findByUserId(String userId);
    Flux<ReminderData> findByUserIdAndEstado(String userId, String estado);
    Flux<ReminderData> findByUserIdAndFechaCreacionGreaterThanEqual(String userId, Long timestamp);

}