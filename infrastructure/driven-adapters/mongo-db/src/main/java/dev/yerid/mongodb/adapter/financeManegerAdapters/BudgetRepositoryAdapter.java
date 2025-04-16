package dev.yerid.mongodb.adapter.financeManegerAdapters;

import dev.yerid.model.financeManager.budget.Budget;
import dev.yerid.model.financeManager.budget.gateways.BudgetRepository;
import dev.yerid.mongodb.data.financeManager.BudgetData;
import dev.yerid.mongodb.repository.financeManeger.BudgetAdapterRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class BudgetRepositoryAdapter implements BudgetRepository {
    private final BudgetAdapterRepository repository;

    public BudgetRepositoryAdapter(BudgetAdapterRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Budget> save(Budget budget) {
        if (budget.getId() == null || budget.getId().isEmpty()) {
            budget = budget.toBuilder()
                    .id(UUID.randomUUID().toString())
                    .build();
        }

        BudgetData data = toData(budget);
        return repository.save(data)
                .map(this::toEntity);
    }

    @Override
    public Mono<Budget> findById(String id) {
        return repository.findById(id)
                .map(this::toEntity);
    }

    @Override
    public Mono<Budget> findByUserId(String userId) {
        return repository.findByUserId(userId)
                .map(this::toEntity);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return repository.deleteById(id);
    }

    @Override
    public Mono<Budget> updateBudgetAmount(String userId, double newAmount) {
        return repository.findByUserId(userId)
                .flatMap(budget -> {
                    budget.setMonto(newAmount);
                    budget.setFechaActualizacion(System.currentTimeMillis());
                    return repository.save(budget);
                })
                .map(this::toEntity)
                .switchIfEmpty(
                        // Si no existe, crear uno nuevo
                        Mono.just(Budget.builder()
                                        .userId(userId)
                                        .monto(newAmount)
                                        .fechaActualizacion(System.currentTimeMillis())
                                        .build())
                                .flatMap(this::save)
                );
    }

    @Override
    public Mono<Budget> increaseBudget(String userId, double amount) {
        return repository.findByUserId(userId)
                .flatMap(budget -> {
                    double newAmount = budget.getMonto() + amount;
                    budget.setMonto(newAmount);
                    budget.setFechaActualizacion(System.currentTimeMillis());
                    return repository.save(budget);
                })
                .map(this::toEntity)
                .switchIfEmpty(
                        // Si no existe, crear uno nuevo
                        Mono.just(Budget.builder()
                                        .userId(userId)
                                        .monto(amount)
                                        .fechaActualizacion(System.currentTimeMillis())
                                        .build())
                                .flatMap(this::save)
                );
    }

    @Override
    public Mono<Budget> decreaseBudget(String userId, double amount) {
        return repository.findByUserId(userId)
                .flatMap(budget -> {
                    double newAmount = budget.getMonto() - amount;
                    // Validar que no quede negativo
                    if (newAmount < 0) {
                        return Mono.error(new IllegalArgumentException("El presupuesto no puede ser negativo"));
                    }
                    budget.setMonto(newAmount);
                    budget.setFechaActualizacion(System.currentTimeMillis());
                    return repository.save(budget);
                })
                .map(this::toEntity);
    }

    private Budget toEntity(BudgetData data) {
        return Budget.builder()
                .id(data.getId())
                .userId(data.getUserId())
                .monto(data.getMonto())
                .fechaActualizacion(data.getFechaActualizacion())
                .build();
    }

    private BudgetData toData(Budget entity) {
        return BudgetData.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .monto(entity.getMonto())
                .fechaActualizacion(entity.getFechaActualizacion())
                .build();
    }
}