package dev.yerid.mongodb.helper;

import org.modelmapper.ModelMapper;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Clase abstracta que proporciona operaciones comunes para adaptadores MongoDB
 * con conversión automática entre entidades de dominio y documentos
 *
 * @param <E> Tipo de la entidad de dominio
 * @param <D> Tipo del documento MongoDB
 * @param <I> Tipo del identificador
 */
public abstract class MongoDBAdapterOperations<E, D, I> {

    protected final ReactiveMongoRepository<D, I> repository;
    protected final ReactiveMongoTemplate mongoTemplate;
    protected final Class<D> documentClass;
    protected final Class<E> entityClass;
    protected final ModelMapper modelMapper;

    protected MongoDBAdapterOperations(
            ReactiveMongoRepository<D, I> repository,
            ReactiveMongoTemplate mongoTemplate,
            Class<D> documentClass,
            Class<E> entityClass) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.documentClass = documentClass;
        this.entityClass = entityClass;
        this.modelMapper = new ModelMapper();

        // Configurar modelMapper para mapeos específicos si es necesario
        configureModelMapper();
    }

    /**
     * Método que pueden sobrescribir las clases hijas para configurar mapeos específicos
     */
    protected void configureModelMapper() {
        // Implementación por defecto vacía
    }

    /**
     * Convierte una entidad de dominio a un documento MongoDB
     */
    protected D toDocument(E entity) {
        if (entity == null) return null;
        return modelMapper.map(entity, documentClass);
    }

    /**
     * Convierte un documento MongoDB a una entidad de dominio
     */
    protected E toEntity(D document) {
        if (document == null) return null;
        return modelMapper.map(document, entityClass);
    }

    public Mono<E> save(E entity) {
        return Mono.just(entity)
                .map(this::toDocument)
                .flatMap(repository::save)
                .map(this::toEntity);
    }

    public Mono<E> findById(I id) {
        return repository.findById(id)
                .map(this::toEntity);
    }

    public Flux<E> findAll() {
        return repository.findAll()
                .map(this::toEntity);
    }

    public Mono<Void> deleteById(I id) {
        return repository.deleteById(id);
    }

    /**
     * Encuentra documentos que coincidan con la consulta y los convierte a entidades
     */
    protected Flux<E> findByQuery(Query query) {
        return mongoTemplate.find(query, documentClass)
                .map(this::toEntity);
    }

    /**
     * Encuentra un único documento que coincida con la consulta y lo convierte a entidad
     */
    protected Mono<E> findOneByQuery(Query query) {
        return mongoTemplate.findOne(query, documentClass)
                .map(this::toEntity);
    }

    /**
     * Crea una consulta para un campo igual a un valor
     */
    protected Query whereEquals(String field, Object value) {
        return Query.query(Criteria.where(field).is(value));
    }

    /**
     * Actualiza campos específicos de un documento basado en una entidad
     */
    protected Mono<E> updateFields(I id, E entity) {
        D document = toDocument(entity);
        return repository.findById(id)
                .flatMap(existingDoc -> {
                    // Aquí podrías implementar lógica para actualizar solo campos no nulos
                    return repository.save(document);
                })
                .map(this::toEntity);
    }
}