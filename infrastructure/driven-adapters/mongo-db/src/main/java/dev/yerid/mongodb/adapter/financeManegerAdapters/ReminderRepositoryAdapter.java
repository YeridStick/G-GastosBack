package dev.yerid.mongodb.adapter.financeManegerAdapters;

import dev.yerid.model.financeManager.reminder.Reminder;
import dev.yerid.model.financeManager.reminder.gateways.ReminderRepository;
import dev.yerid.mongodb.data.financeManager.ReminderData;
import dev.yerid.mongodb.repository.financeManeger.ReminderAdapterRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class ReminderRepositoryAdapter implements ReminderRepository {
    private final ReminderAdapterRepository repository;

    public ReminderRepositoryAdapter(ReminderAdapterRepository repository) {
        this.repository = repository;
    }

    @Override
    public Flux<Reminder> findByUserIdAndUpdatedSince(String userId, long timestamp) {
        // Usando fechaCreacion como campo de referencia para sincronización
        return repository.findByUserIdAndFechaCreacionGreaterThanEqual(userId, timestamp)
                .map(this::toEntity);
    }

    @Override
    public Mono<Reminder> save(Reminder reminder) {
        if (reminder.getId() == null || reminder.getId().isEmpty()) {
            reminder = reminder.toBuilder()
                    .id(UUID.randomUUID().toString())
                    .build();
        }

        ReminderData data = toData(reminder);
        return repository.save(data)
                .map(this::toEntity);
    }

    @Override
    public Mono<Reminder> findById(String id) {
        return repository.findById(id)
                .map(this::toEntity);
    }

    @Override
    public Flux<Reminder> findByUserId(String userId) {
        return repository.findByUserId(userId)
                .map(this::toEntity);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return repository.deleteById(id);
    }

    @Override
    public Flux<Reminder> findByUserIdAndEstado(String userId, String estado) {
        return repository.findByUserIdAndEstado(userId, estado)
                .map(this::toEntity);
    }

    @Override
    public Flux<Reminder> findUpcomingReminders(String userId, Long currentDate, int daysAhead) {
        // Calcular fecha límite (fecha actual + días de anticipación)
        long limitDate = currentDate + (daysAhead * 24 * 60 * 60 * 1000);

        return repository.findByUserIdAndEstado(userId, "pendiente")
                .filter(reminder ->
                        reminder.getFechaVencimiento() > currentDate &&
                                reminder.getFechaVencimiento() <= limitDate)
                .map(this::toEntity);
    }

    @Override
    public Mono<Reminder> updateStatus(String id, String newStatus) {
        return repository.findById(id)
                .flatMap(reminder -> {
                    reminder.setEstado(newStatus);
                    return repository.save(reminder);
                })
                .map(this::toEntity);
    }

    @Override
    public Mono<Reminder> createRecurringReminder(Reminder reminder) {
        // Validar que sea recurrente
        if (!reminder.isEsRecurrente()) {
            return Mono.error(new IllegalArgumentException("El recordatorio no es recurrente"));
        }

        // Asignar ID nuevo
        reminder = reminder.toBuilder()
                .id(UUID.randomUUID().toString())
                .build();

        ReminderData data = toData(reminder);
        return repository.save(data)
                .map(this::toEntity);
    }

    private Reminder toEntity(ReminderData data) {
        return Reminder.builder()
                .id(data.getId())
                .titulo(data.getTitulo())
                .descripcion(data.getDescripcion())
                .monto(data.getMonto())
                .fechaVencimiento(data.getFechaVencimiento())
                .categoria(data.getCategoria())
                .esRecurrente(data.isEsRecurrente())
                .frecuencia(data.getFrecuencia())
                .diasAnticipacion(data.getDiasAnticipacion())
                .fechaCreacion(data.getFechaCreacion())
                .estado(data.getEstado())
                .userId(data.getUserId())
                .build();
    }

    private ReminderData toData(Reminder entity) {
        return ReminderData.builder()
                .id(entity.getId())
                .titulo(entity.getTitulo())
                .descripcion(entity.getDescripcion())
                .monto(entity.getMonto())
                .fechaVencimiento(entity.getFechaVencimiento())
                .categoria(entity.getCategoria())
                .esRecurrente(entity.isEsRecurrente())
                .frecuencia(entity.getFrecuencia())
                .diasAnticipacion(entity.getDiasAnticipacion())
                .fechaCreacion(entity.getFechaCreacion())
                .estado(entity.getEstado())
                .userId(entity.getUserId())
                .build();
    }
}