package dev.yerid.mongodb.adapter.financeManegerAdapters;

import dev.yerid.model.financeManager.categories.Categories;
import dev.yerid.model.financeManager.categories.gateways.CategoriesRepository;
import dev.yerid.mongodb.data.financeManager.CategoriesData;
import dev.yerid.mongodb.repository.financeManeger.CategoriesAdapterRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CategoriesRepositoryAdapter implements CategoriesRepository {

    private final CategoriesAdapterRepository repository;

    public CategoriesRepositoryAdapter(CategoriesAdapterRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Categories> save(Categories categories) {
        if (categories.getId() == null || categories.getId().isEmpty()) {
            categories = categories.toBuilder()
                    .id(UUID.randomUUID().toString())
                    .build();
        }

        CategoriesData data = toData(categories);
        return repository.save(data)
                .map(this::toEntity);
    }

    @Override
    public Flux<Categories> findByUserIdAndUpdatedSince(String userId, long timestamp) {
        // Dado que las categorías podrían no tener un campo de timestamp,
        // podrías cargar todas y filtrarlas en la aplicación, o agregar
        // un campo updatedAt a tu entidad CategoriesData

        // Solución temporal: cargar todas las categorías del usuario
        return repository.findByUserId(userId)
                .map(this::toEntity);

        // Solución ideal si agregas updatedAt:
        // return repository.findByUserIdAndUpdatedAtGreaterThanEqual(userId, timestamp)
        //         .map(this::toEntity);
    }

    @Override
    public Mono<Categories> findById(String id) {
        return repository.findById(id)
                .map(this::toEntity);
    }

    @Override
    public Flux<Categories> findByUserId(String userId) {
        return repository.findByUserId(userId)
                .map(this::toEntity);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return repository.deleteById(id);
    }

    private Categories toEntity(CategoriesData data) {
        return Categories.builder()
                .id(data.getId())
                .nombre(data.getNombre())
                .icono(data.getIcono())
                .color(data.getColor())
                .userId(data.getUserId())
                .build();
    }

    private CategoriesData toData(Categories entity) {
        return CategoriesData.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .icono(entity.getIcono())
                .color(entity.getColor())
                .userId(entity.getUserId())
                .build();
    }
}