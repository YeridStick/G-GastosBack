package dev.yerid.mongodb.adapter.financeManegerAdapters;

import dev.yerid.model.financeManager.expenses.Expenses;
import dev.yerid.model.financeManager.expenses.gateways.ExpensesRepository;
import dev.yerid.mongodb.data.financeManager.ExpensesData;
import dev.yerid.mongodb.repository.financeManeger.ExpensesAdapterRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.logging.Logger;

@Component
public class ExpensesRepositoryAdapter implements ExpensesRepository {
    private static final Logger logger = Logger.getLogger(ExpensesRepositoryAdapter.class.getName());
    private final ExpensesAdapterRepository repository;

    public ExpensesRepositoryAdapter(ExpensesAdapterRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Expenses> findByRecordatorioId(String recordatorioId) {
        return repository.findByRecordatorioId(recordatorioId)
                .map(this::toEntity);
    }

    @Override
    public Mono<Expenses> save(Expenses expenses) {
        /*if (expenses.getUserId() == null || expenses.getUserId().isEmpty()) {
            logger.warning("Intento de guardar gasto sin userId: " + expenses.getNombreG());
            return Mono.error(new IllegalArgumentException("El userId no puede estar vacío"));
        }*/

        logger.info("=== INICIO DE GUARDADO DE GASTO ===");
        logger.info("ID: " + expenses.getId());
        logger.info("Nombre: " + expenses.getNombreG());
        logger.info("Monto: " + expenses.getGasto());
        logger.info("Categoría: " + expenses.getCategoria());
        logger.info("Usuario: " + expenses.getUserId());
        logger.info("=== FIN DE DATOS DE GASTO ===");

        return repository.save(toData(expenses))
                .map(this::toEntity);
    }

    @Override
    public Mono<Expenses> findById(String id) {
        return repository.findById(id)
                .map(this::toEntity);
    }

    @Override
    public Flux<Expenses> findByUserIdAndUpdatedSince(String userId, long timestamp) {
        logger.info("Buscando gastos para userId: " + userId + " desde timestamp: " + timestamp);
        return repository.findByUserIdAndFechaGreaterThanEqual(userId, timestamp)
                .map(this::toEntity);
    }

    @Override
    public Flux<Expenses> findByUserId(String userId) {
        logger.info("Buscando todos los gastos para userId: " + userId);
        return repository.findByUserId(userId)
                .map(this::toEntity);
    }

    @Override
    public Flux<Expenses> findByUserIdAndCategoria(String userId, String categoria) {
        logger.info("Buscando gastos para userId: " + userId + " y categoría: " + categoria);
        return repository.findByUserIdAndCategoria(userId, categoria)
                .map(this::toEntity);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return repository.deleteById(id);
    }

    private Expenses toEntity(ExpensesData data) {
        return Expenses.builder()
                .id(data.getId())
                .nombreG(data.getNombreG())
                .gasto(data.getGasto())
                .categoria(data.getCategoria())
                .fecha(data.getFecha())
                .userId(data.getUserId())
                .origen(data.getOrigen())
                .recordatorioId(data.getRecordatorioId())
                .build();
    }

    private ExpensesData toData(Expenses entity) {
        return ExpensesData.builder()
                .id(entity.getId())
                .nombreG(entity.getNombreG())
                .gasto(entity.getGasto())
                .categoria(entity.getCategoria())
                .fecha(entity.getFecha())
                .userId(entity.getUserId())
                .origen(entity.getOrigen())
                .recordatorioId(entity.getRecordatorioId())
                .build();
    }
}