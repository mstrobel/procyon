package com.strobel.collections;

import com.strobel.core.VerifyArgument;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author strobelm
 */
public abstract class Cache<K, V> {
    protected Cache() {
    }

    public boolean contains(final K key) {
        return get(key) != null;
    }

    public boolean contains(final K key, final V value) {
        final V cachedValue = get(key);
        return cachedValue != null && cachedValue.equals(value);
    }

    public abstract boolean replace(final K key, final V expectedValue, final V updatedValue);

    public abstract V get(final K key);
    public abstract V cache(final K key, final V value);

    public static <K, V> Cache<K, V> createTopLevelCache() {
        return new TopLevelCache<>();
    }

    public static <K, V> Cache<K, V> createSatelliteCache() {
        return new SatelliteCache<>();
    }

    public static <K, V> Cache<K, V> createSatelliteCache(final Cache<K, V> parent) {
        return new SatelliteCache<>(VerifyArgument.notNull(parent, "parent"));
    }

    public static <K, V> Cache<K, V> createThreadLocalCache() {
        return new ThreadLocalCache<>();
    }

    public static <K, V> Cache<K, V> createThreadLocalCache(final Cache<K, V> parent) {
        return new ThreadLocalCache<>(VerifyArgument.notNull(parent, "parent"));
    }
}

final class TopLevelCache<K, V> extends Cache<K, V> {
    private final ConcurrentHashMap<K, V> _cache = new ConcurrentHashMap<>();

    @Override
    public V cache(final K key, final V value) {
        final V cachedValue = _cache.putIfAbsent(key, value);
        return cachedValue != null ? cachedValue : value;
    }

    @Override
    public boolean replace(final K key, final V expectedValue, final V updatedValue) {
        if (expectedValue == null) {
            return _cache.putIfAbsent(key, updatedValue) == null;
        }
        return _cache.replace(key, expectedValue, updatedValue);
    }

    @Override
    public V get(final K key) {
        return _cache.get(key);
    }
}

final class SatelliteCache<K, V> extends Cache<K, V> {
    private final Cache<K, V> _parent;
    private final HashMap<K, V> _cache = new HashMap<>();

    public SatelliteCache() {
        _parent = null;
    }

    @Override
    public boolean replace(final K key, final V expectedValue, final V updatedValue) {
        if (_parent != null && !_parent.replace(key, expectedValue, updatedValue)) {
            return false;
        }
        _cache.put(key, updatedValue);
        return true;
    }

    public SatelliteCache(final Cache<K, V> parent) {
        _parent = parent;
    }

    @Override
    public V cache(final K key, final V value) {
        V cachedValue = _cache.get(key);

        if (cachedValue != null) {
            return cachedValue;
        }

        if (_parent != null) {
            cachedValue = _parent.cache(key, value);
        }
        else {
            cachedValue = value;
        }

        _cache.put(key, cachedValue);

        return cachedValue;
    }

    @Override
    public V get(final K key) {
        V cachedValue = _cache.get(key);

        if (cachedValue != null) {
            return cachedValue;
        }

        if (_parent != null) {
            cachedValue = _parent.get(key);

            if (cachedValue != null) {
                _cache.put(key, cachedValue);
            }
        }

        return cachedValue;
    }
}

final class ThreadLocalCache<K, V> extends Cache<K, V> {
    private final Cache<K, V> _parent;

    @SuppressWarnings("ThreadLocalNotStaticFinal")
    private final ThreadLocal<SatelliteCache<K, V>> _threadCaches = new ThreadLocal<SatelliteCache<K, V>>() {
        @Override
        protected SatelliteCache<K, V> initialValue() {
            return new SatelliteCache<>(_parent);
        }
    };

    public ThreadLocalCache() {
        _parent = null;
    }

    @Override
    public boolean replace(final K key, final V expectedValue, final V updatedValue) {
        return _threadCaches.get().replace(key, expectedValue, updatedValue);
    }

    public ThreadLocalCache(final Cache<K, V> parent) {
        _parent = parent;
    }

    @Override
    public V cache(final K key, final V value) {
        return _threadCaches.get().cache(key, value);
    }

    @Override
    public V get(final K key) {
        return _threadCaches.get().get(key);
    }
}
