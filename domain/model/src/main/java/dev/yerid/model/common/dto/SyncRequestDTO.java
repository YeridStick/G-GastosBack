package dev.yerid.model.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncRequestDTO {
    private String email;
    private Map<String, Object> data;
    private Map<String, Object> eliminados;
    private long timestamp;
}