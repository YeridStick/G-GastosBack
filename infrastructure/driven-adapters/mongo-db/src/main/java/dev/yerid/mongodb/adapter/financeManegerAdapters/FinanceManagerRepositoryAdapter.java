package dev.yerid.mongodb.adapter.financeManegerAdapters;

import dev.yerid.model.financeManager.finance.FinanceManager;
import dev.yerid.model.financeManager.finance.gateways.FinanceManagerRepository;
import dev.yerid.mongodb.data.financeManager.*;
import dev.yerid.mongodb.repository.financeManeger.FinanceManagerAdapterRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.logging.Logger;

@Component
public class FinanceManagerRepositoryAdapter implements FinanceManagerRepository {
    private static final Logger logger = Logger.getLogger(FinanceManagerRepositoryAdapter.class.getName());

    private final FinanceManagerAdapterRepository repository;
    private final FinanceManagerDataConverter converter;
    private final FinanceManagerDataExtractor extractor;
    private final FinanceManagerQueryExecutor queryExecutor;

    public FinanceManagerRepositoryAdapter(
            FinanceManagerAdapterRepository repository,
            FinanceManagerDataConverter converter,
            FinanceManagerDataExtractor extractor,
            FinanceManagerQueryExecutor queryExecutor) {
        this.repository = repository;
        this.converter = converter;
        this.extractor = extractor;
        this.queryExecutor = queryExecutor;
    }

    /**
     * Obtiene el documento financiero del usuario
     */
    @Override
    public Mono<FinanceManager> findByUserId(String userId) {
        return repository.findByUserId(userId)
                .map(converter::toEntity)
                .switchIfEmpty(Mono.defer(() -> {
                    // Si no existe, crear un documento vacío
                    FinanceManagerData emptyData = FinanceManagerData.createEmpty(userId);
                    return repository.save(emptyData)
                            .map(converter::toEntity);
                }));
    }

    /**
     * Actualiza o crea el documento completo
     */
    @Override
    public Mono<Void> batchUpdate(String userId, Map<String, Object> data, Map<String, Object> eliminados) {
        return repository.existsByUserId(userId)
                .flatMap(exists -> {
                    if (exists) {
                        // Si existe, hacer un update parcial
                        return updateDocumentPartially(userId, data, eliminados);
                    } else {
                        // Si no existe, crear nuevo documento
                        return createNewDocument(userId, data, eliminados);
                    }
                });
    }

    /**
     * Crea un nuevo documento para el usuario
     */
    private Mono<Void> createNewDocument(String userId, Map<String, Object> data, Map<String, Object> eliminados) {
        FinanceManagerData document = extractor.prepareDocument(userId, data, eliminados);

        return repository.save(document)
                .doOnSuccess(savedDoc -> logger.info("Documento creado exitosamente para usuario: " + userId))
                .doOnError(e -> logger.severe("Error al crear documento para usuario: " + userId + " - " + e.getMessage()))
                .then();
    }

    /**
     * Actualiza partes específicas del documento sin reemplazar todo
     */
    private Mono<Void> updateDocumentPartially(String userId, Map<String, Object> data, Map<String, Object> eliminados) {
        return queryExecutor.updateDocument(userId, data, eliminados)
                .doOnSuccess(result -> logger.info("Documento actualizado parcialmente para usuario: " + userId))
                .doOnError(e -> logger.severe("Error al actualizar documento para usuario: " + userId + " - " + e.getMessage()))
                .then();
    }
}