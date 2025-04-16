package dev.yerid.model.financeManager.reminder.gateways;

import dev.yerid.model.financeManager.reminder.Reminder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReminderRepository {
    Mono<Reminder> save(Reminder reminder);
    Mono<Reminder> findById(String id);
    Flux<Reminder> findByUserId(String userId);
    Mono<Void> deleteById(String id);
    Flux<Reminder> findByUserIdAndUpdatedSince(String userId, long timestamp);
    Flux<Reminder> findByUserIdAndEstado(String userId, String estado);
    Flux<Reminder> findUpcomingReminders(String userId, Long currentDate, int daysAhead);
    Mono<Reminder> updateStatus(String id, String newStatus);
    Mono<Reminder> createRecurringReminder(Reminder reminder);
}
