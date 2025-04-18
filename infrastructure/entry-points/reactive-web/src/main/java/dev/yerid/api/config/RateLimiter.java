package dev.yerid.api.config;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimiter {
    // Almacena el recuento de solicitudes por usuario
    private final Map<String, AtomicInteger> requestCountMap = new ConcurrentHashMap<>();

    // Almacena el tiempo de la última solicitud por usuario
    private final Map<String, Long> lastRequestTimeMap = new ConcurrentHashMap<>();

    // Almacena el token de sesión activa por usuario
    private final Map<String, String> activeSessionMap = new ConcurrentHashMap<>();

    // Configuración: máximo de solicitudes permitidas en el período de tiempo
    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_SIZE_MS = 10000; // 10 segundos

    // Tiempo de expiración de la sesión
    private static final long SESSION_EXPIRY_MS = 1800000; // 30 minutos

    /**
     * Verifica si una solicitud debe ser limitada según la tasa de peticiones
     */
    public Mono<Boolean> isRateLimited(String userId) {
        long currentTime = System.currentTimeMillis();

        // Obtiene o crea un contador para este usuario
        AtomicInteger counter = requestCountMap.computeIfAbsent(userId, k -> new AtomicInteger(0));

        // Obtiene el tiempo de la última solicitud
        Long lastRequestTime = lastRequestTimeMap.get(userId);

        // Si ha pasado más tiempo que la ventana, reinicia el contador
        if (lastRequestTime != null && currentTime - lastRequestTime > WINDOW_SIZE_MS) {
            counter.set(0);
        }

        // Actualiza el tiempo de la última solicitud
        lastRequestTimeMap.put(userId, currentTime);

        // Incrementa el contador y comprueba si supera el límite
        int count = counter.incrementAndGet();
        return Mono.just(count > MAX_REQUESTS);
    }

    /**
     * Registra una sesión activa para un usuario
     */
    public void registerSession(String userId, String sessionToken) {
        activeSessionMap.put(userId, sessionToken);

        // Programar limpieza después del tiempo de expiración
        scheduleSessionCleanup(userId, sessionToken);
    }

    /**
     * Verifica si una sesión es la activa para un usuario
     */
    public boolean isActiveSession(String userId, String sessionToken) {
        String activeToken = activeSessionMap.get(userId);
        return activeToken != null && activeToken.equals(sessionToken);
    }

    /**
     * Cierra una sesión específica
     */
    public void closeSession(String userId, String sessionToken) {
        String activeToken = activeSessionMap.get(userId);
        if (activeToken != null && activeToken.equals(sessionToken)) {
            activeSessionMap.remove(userId);
        }
    }

    /**
     * Programar limpieza de sesión después del tiempo de expiración
     */
    private void scheduleSessionCleanup(String userId, String sessionToken) {
        // Aquí podrías usar un scheduler de Spring o similar
        // Para simplificar, usamos un thread
        new Thread(() -> {
            try {
                Thread.sleep(SESSION_EXPIRY_MS);
                closeSession(userId, sessionToken);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}