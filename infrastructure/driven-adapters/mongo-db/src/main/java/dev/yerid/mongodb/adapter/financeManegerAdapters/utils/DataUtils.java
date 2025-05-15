package dev.yerid.mongodb.adapter.financeManegerAdapters.utils;


import java.util.UUID;

/**
 * Clase de utilidades para la manipulación de datos
 */
public class DataUtils {

    /**
     * Convierte un objeto a String
     */
    public static String toString(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    /**
     * Convierte un objeto a double
     */
    public static double parseToDouble(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    /**
     * Convierte un objeto a int
     */
    public static int parseToInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    /**
     * Convierte un objeto a Long
     */
    public static Long parseToLong(Object value, Long defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    /**
     * Convierte un objeto a boolean
     */
    public static boolean parseToBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }

        return defaultValue;
    }

    /**
     * Parsea un objeto fecha a Long
     */
    public static Long parseFecha(Object fechaObj) {
        if (fechaObj == null) {
            return System.currentTimeMillis();
        }

        // Si ya es Long, devolverlo directamente
        if (fechaObj instanceof Long) {
            return (Long) fechaObj;
        }

        // Si es un Number (Integer, Double, etc.), convertirlo a Long
        if (fechaObj instanceof Number) {
            return ((Number) fechaObj).longValue();
        }

        // Si es String, puede ser un timestamp o una fecha ISO
        if (fechaObj instanceof String) {
            String fechaStr = (String) fechaObj;
            try {
                // Primero intentar parsear como número
                return Long.parseLong(fechaStr);
            } catch (NumberFormatException e) {
                // Si no es un número, intentar como fecha ISO
                try {
                    // Eliminar la 'Z' del final si existe
                    if (fechaStr.endsWith("Z")) {
                        fechaStr = fechaStr.substring(0, fechaStr.length() - 1);
                    }

                    // Convertir fecha ISO a milisegundos
                    return java.time.Instant.parse(fechaStr).toEpochMilli();
                } catch (Exception ex) {
                    // Si no se puede parsear, devolver la hora actual
                    return System.currentTimeMillis();
                }
            }
        }

        // En caso de formato desconocido, usar la fecha actual
        return System.currentTimeMillis();
    }

    /**
     * Genera un ID para una categoría basado en su nombre
     */
    public static String generateCategoryId(String nombre) {
        if (nombre == null || nombre.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return nombre.replaceAll(" ", "").toLowerCase() + "-" + System.currentTimeMillis() + "-" + (int)(Math.random()*1000);
    }
}
