package dev.yerid.mongodb.adapter.financeManegerAdapters;

import com.mongodb.client.result.UpdateResult;
import dev.yerid.mongodb.data.financeManager.FinanceManagerData;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Clase utilitaria para ejecutar operaciones de MongoDB
 */
@Component
public class FinanceManagerQueryExecutor {
    private static final Logger logger = Logger.getLogger(FinanceManagerQueryExecutor.class.getName());

    private final ReactiveMongoTemplate mongoTemplate;
    private final FinanceManagerDataExtractor extractor;

    public FinanceManagerQueryExecutor(ReactiveMongoTemplate mongoTemplate, FinanceManagerDataExtractor extractor) {
        this.mongoTemplate = mongoTemplate;
        this.extractor = extractor;
    }

    /**
     * Actualiza un documento usando los datos proporcionados
     */
    public Mono<UpdateResult> updateDocument(String userId, Map<String, Object> data, Map<String, Object> eliminados) {
        // Crear la consulta para buscar el documento
        Query query = createUserIdQuery(userId);

        // Preparar el objeto Update con los cambios
        Update update = extractor.prepareUpdate(data, eliminados);

        // Ejecutar la actualización
        return mongoTemplate.updateFirst(query, update, FinanceManagerData.class)
                .doOnSuccess(result -> {
                    if (result.getModifiedCount() > 0) {
                        logger.info("Documento actualizado correctamente para usuario: " + userId);
                    } else {
                        logger.warning("No se encontró documento para actualizar para usuario: " + userId);
                    }
                })
                .doOnError(e -> logger.severe("Error al actualizar documento para usuario: " + userId + " - " + e.getMessage()));
    }

    /**
     * Encuentra un documento por userId y lo actualiza parcialmente
     */
    public Mono<FinanceManagerData> findAndUpdate(String userId, Update update) {
        Query query = createUserIdQuery(userId);
        return mongoTemplate.findAndModify(query, update, FinanceManagerData.class)
                .doOnSuccess(result -> {
                    if (result != null) {
                        logger.info("Documento actualizado y recuperado para usuario: " + userId);
                    } else {
                        logger.warning("No se encontró documento para usuario: " + userId);
                    }
                })
                .doOnError(e -> logger.severe("Error al buscar y actualizar documento: " + e.getMessage()));
    }

    /**
     * Elimina un elemento específico de una colección
     */
    public Mono<UpdateResult> deleteItemFromCollection(String userId, String collection, String itemId) {
        Query query = createUserIdQuery(userId);
        Update update = new Update();

        // Eliminar el elemento de la colección
        update.unset(collection + "." + itemId);

        // Agregar el ID a la lista de eliminados
        update.addToSet("eliminados." + collection, itemId);

        return mongoTemplate.updateFirst(query, update, FinanceManagerData.class)
                .doOnSuccess(result -> {
                    if (result.getModifiedCount() > 0) {
                        logger.info("Elemento " + itemId + " eliminado de " + collection + " para usuario: " + userId);
                    } else {
                        logger.warning("No se pudo eliminar elemento " + itemId + " de " + collection);
                    }
                })
                .doOnError(e -> logger.severe("Error al eliminar elemento: " + e.getMessage()));
    }

    /**
     * Agrega un elemento a una colección
     */
    public Mono<UpdateResult> addItemToCollection(String userId, String collection, String itemId, Object item) {
        Query query = createUserIdQuery(userId);
        Update update = new Update();

        // Agregar el elemento a la colección
        update.set(collection + "." + itemId, item);

        return mongoTemplate.updateFirst(query, update, FinanceManagerData.class)
                .doOnSuccess(result -> {
                    if (result.getModifiedCount() > 0) {
                        logger.info("Elemento " + itemId + " agregado a " + collection + " para usuario: " + userId);
                    } else {
                        logger.warning("No se pudo agregar elemento " + itemId + " a " + collection);
                    }
                })
                .doOnError(e -> logger.severe("Error al agregar elemento: " + e.getMessage()));
    }

    /**
     * Actualiza el presupuesto del usuario
     */
    public Mono<UpdateResult> updateBudget(String userId, double amount) {
        Query query = createUserIdQuery(userId);
        Update update = new Update();

        update.set("presupuesto.monto", amount);
        update.set("presupuesto.fechaActualizacion", System.currentTimeMillis());

        return mongoTemplate.updateFirst(query, update, FinanceManagerData.class)
                .doOnSuccess(result -> {
                    if (result.getModifiedCount() > 0) {
                        logger.info("Presupuesto actualizado para usuario: " + userId);
                    } else {
                        logger.warning("No se pudo actualizar presupuesto para usuario: " + userId);
                    }
                })
                .doOnError(e -> logger.severe("Error al actualizar presupuesto: " + e.getMessage()));
    }

    /**
     * Crea una consulta por userId
     */
    private Query createUserIdQuery(String userId) {
        return new Query(Criteria.where("userId").is(userId));
    }
}