package dev.yerid.model.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO para las respuestas de sincronización.
 * Contiene los datos sincronizados del usuario pero no incluye
 * información sobre elementos eliminados, ya que estos ya habrán
 * sido procesados por el backend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncResponseDTO {
    /**
     * Datos del usuario organizados por tipo
     * (ObjetosGastos, MetasAhorro, categorias, recordatorios, PresupuestoLS, IngresosExtra)
     */
    private Map<String, Object> data;

    /**
     * Marca de tiempo de la sincronización
     */
    private long timestamp;

    private boolean sessionActive;
}