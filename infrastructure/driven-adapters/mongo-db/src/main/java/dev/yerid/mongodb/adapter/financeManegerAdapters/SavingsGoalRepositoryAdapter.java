package dev.yerid.mongodb.adapter.financeManegerAdapters;

import dev.yerid.model.financeManager.savingsgoal.SavingsGoal;
import dev.yerid.model.financeManager.savingsgoal.gateways.SavingsGoalRepository;
import dev.yerid.mongodb.data.financeManager.SavingsGoalData;
import dev.yerid.mongodb.repository.financeManeger.SavingsGoalAdapterRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class SavingsGoalRepositoryAdapter implements SavingsGoalRepository {
    private final SavingsGoalAdapterRepository repository;

    public SavingsGoalRepositoryAdapter(SavingsGoalAdapterRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<SavingsGoal> save(SavingsGoal savingsGoal) {
        if (savingsGoal.getId() == null || savingsGoal.getId().isEmpty()) {
            savingsGoal = savingsGoal.toBuilder()
                    .id(UUID.randomUUID().toString())
                    .build();
        }

        SavingsGoalData data = toData(savingsGoal);
        return repository.save(data)
                .map(this::toEntity);
    }

    @Override
    public Flux<SavingsGoal> findByUserIdAndUpdatedSince(String userId, long timestamp) {
        // Si tienes un campo updatedAt específico:
        // return repository.findByUserIdAndUpdatedAtGreaterThanEqual(userId, timestamp)
        //         .map(this::toEntity);

        // Si usas el campo creada como aproximación:
        return repository.findByUserIdAndCreadaGreaterThanEqual(userId, timestamp)
                .map(this::toEntity);
    }

    @Override
    public Mono<SavingsGoal> findById(String id) {
        return repository.findById(id)
                .map(this::toEntity);
    }

    @Override
    public Flux<SavingsGoal> findByUserId(String userId) {
        return repository.findByUserId(userId)
                .map(this::toEntity);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return repository.deleteById(id);
    }

    @Override
    public Flux<SavingsGoal> findCompletedGoalsByUserId(String userId) {
        return repository.findByUserIdAndCompletada(userId, true)
                .map(this::toEntity);
    }

    @Override
    public Flux<SavingsGoal> findPendingGoalsByUserId(String userId) {
        return repository.findByUserIdAndCompletada(userId, false)
                .map(this::toEntity);
    }

    @Override
    public Mono<SavingsGoal> updateSavedAmount(String id, double newAmount) {
        return repository.findById(id)
                .flatMap(goal -> {
                    goal.setAhorroAcumulado(newAmount);
                    // Actualizar estado de completado si alcanza la meta
                    if (newAmount >= goal.getMonto()) {
                        goal.setCompletada(true);
                    }
                    return repository.save(goal);
                })
                .map(this::toEntity);
    }

    private SavingsGoal toEntity(SavingsGoalData data) {
        return SavingsGoal.builder()
                .id(data.getId())
                .nombre(data.getNombre())
                .monto(data.getMonto())
                .fechaObjetivo(data.getFechaObjetivo())
                .descripcion(data.getDescripcion())
                .creada(data.getCreada())
                .ahorroAcumulado(data.getAhorroAcumulado())
                .ahorroSemanal(data.getAhorroSemanal())
                .ahorroMensual(data.getAhorroMensual())
                .ahorroAnual(data.getAhorroAnual())
                .diasRestantes(data.getDiasRestantes())
                .completada(data.isCompletada())
                .userId(data.getUserId())
                .build();
    }

    private SavingsGoalData toData(SavingsGoal entity) {
        return SavingsGoalData.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .monto(entity.getMonto())
                .fechaObjetivo(entity.getFechaObjetivo())
                .descripcion(entity.getDescripcion())
                .creada(entity.getCreada())
                .ahorroAcumulado(entity.getAhorroAcumulado())
                .ahorroSemanal(entity.getAhorroSemanal())
                .ahorroMensual(entity.getAhorroMensual())
                .ahorroAnual(entity.getAhorroAnual())
                .diasRestantes(entity.getDiasRestantes())
                .completada(entity.isCompletada())
                .userId(entity.getUserId())
                .build();
    }
}