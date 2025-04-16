package dev.yerid.mongodb.adapter.financeManegerAdapters;

import dev.yerid.model.financeManager.extraincome.ExtraIncome;
import dev.yerid.model.financeManager.extraincome.gateways.ExtraIncomeRepository;
import dev.yerid.mongodb.data.financeManager.ExtraIncomeData;
import dev.yerid.mongodb.repository.financeManeger.ExtraIncomeAdapterRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Component
public class ExtraIncomeRepositoryAdapter implements ExtraIncomeRepository {
    private final ExtraIncomeAdapterRepository repository;

    public ExtraIncomeRepositoryAdapter(ExtraIncomeAdapterRepository repository) {
        this.repository = repository;
    }

    @Override
    public Flux<ExtraIncome> findByUserIdAndUpdatedSince(String userId, long timestamp) {
        // Usando fecha como campo de referencia
        return repository.findByUserIdAndFechaGreaterThanEqual(userId, timestamp)
                .map(this::toEntity);
    }

    @Override
    public Mono<ExtraIncome> save(ExtraIncome extraIncome) {
        if (extraIncome.getId() == null || extraIncome.getId().isEmpty()) {
            extraIncome = extraIncome.toBuilder()
                    .id(UUID.randomUUID().toString())
                    .build();
        }

        ExtraIncomeData data = toData(extraIncome);
        return repository.save(data)
                .map(this::toEntity);
    }

    @Override
    public Mono<ExtraIncome> findById(String id) {
        return repository.findById(id)
                .map(this::toEntity);
    }

    @Override
    public Flux<ExtraIncome> findByUserId(String userId) {
        return repository.findByUserId(userId)
                .map(this::toEntity);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return repository.deleteById(id);
    }

    /**
     * Encuentra todos los ingresos extra de un usuario en un rango de fechas
     */
    @Override
    public Flux<ExtraIncome> findByUserIdAndDateRange(String userId, Long startDate, Long endDate) {
        return repository.findByUserId(userId)
                .filter(data -> data.getFecha() >= startDate && data.getFecha() <= endDate)
                .map(this::toEntity);
    }

    /**
     * Calcula el total de ingresos extra para un usuario
     */
    @Override
    public Mono<Double> getTotalExtraIncomeByUserId(String userId) {
        return repository.findByUserId(userId)
                .map(ExtraIncomeData::getMonto)
                .reduce(0.0, Double::sum);
    }

    /**
     * Calcula el total de ingresos extra de un usuario en un periodo específico (mes actual)
     */
    @Override
    public Mono<Double> getCurrentMonthExtraIncomeByUserId(String userId) {
        // Obtener el primer día del mes actual
        LocalDateTime firstDayOfMonth = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0);

        // Convertir a timestamp para comparar con los datos
        Long startOfMonth = firstDayOfMonth
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        // Obtener timestamp actual
        Long currentTime = System.currentTimeMillis();

        return repository.findByUserId(userId)
                .filter(data -> data.getFecha() >= startOfMonth && data.getFecha() <= currentTime)
                .map(ExtraIncomeData::getMonto)
                .reduce(0.0, Double::sum);
    }

    /**
     * Actualiza la descripción de un ingreso extra
     */
    public Mono<ExtraIncome> updateDescription(String id, String newDescription) {
        return repository.findById(id)
                .flatMap(existingData -> {
                    existingData.setDescripcion(newDescription);
                    return repository.save(existingData);
                })
                .map(this::toEntity);
    }

    private ExtraIncome toEntity(ExtraIncomeData data) {
        return ExtraIncome.builder()
                .id(data.getId())
                .monto(data.getMonto())
                .descripcion(data.getDescripcion())
                .fecha(data.getFecha())
                .userId(data.getUserId())
                .build();
    }

    private ExtraIncomeData toData(ExtraIncome entity) {
        return ExtraIncomeData.builder()
                .id(entity.getId())
                .monto(entity.getMonto())
                .descripcion(entity.getDescripcion())
                .fecha(entity.getFecha())
                .userId(entity.getUserId())
                .build();
    }
}