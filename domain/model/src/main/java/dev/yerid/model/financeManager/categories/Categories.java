package dev.yerid.model.financeManager.categories;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class Categories {
    private String id;
    private String nombre;
    private String icono;
    private String color;
    private String userId;
}
