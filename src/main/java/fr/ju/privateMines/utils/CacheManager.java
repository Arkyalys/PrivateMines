package fr.ju.privateMines.utils;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
public class CacheManager {
    private final Map<String, CacheEntry> cache;
    private final long defaultExpirationTime;
    public CacheManager() {
        this.cache = new HashMap<>();
        this.defaultExpirationTime = TimeUnit.MINUTES.toMillis(5); 
    }
    public void put(String key, Object value) {
        put(key, value, defaultExpirationTime);
    }
    public void put(String key, Object value, long expirationTime) {
        cache.put(key, new CacheEntry(value, System.currentTimeMillis() + expirationTime));
    }
    public Object get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return entry.getValue();
    }
    public void remove(String key) {
        cache.remove(key);
    }
    public void clear() {
        cache.clear();
    }
    private static class CacheEntry {
        private final Object value;
        private final long expirationTime;
        public CacheEntry(Object value, long expirationTime) {
            this.value = value;
            this.expirationTime = expirationTime;
        }
        public Object getValue() {
            return value;
        }
        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
} 