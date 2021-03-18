package com.gentics.mesh.plugin.export.util;

import io.reactivex.Single;
import io.reactivex.subjects.ReplaySubject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ObjectCache<T> {

	private static final Logger log = LoggerFactory.getLogger(ObjectCache.class);

	private Long cacheTimeMs;
	private Map<String, CacheEntry<T>> cache = new ConcurrentHashMap<>();

	public ObjectCache(Long cacheTimeMs) {
		this.cacheTimeMs = cacheTimeMs;
	}

	private void kickOld() {
		Long now = System.currentTimeMillis();
		List<String> configsToRemove = this.cache.entrySet().stream()
			.filter(entry -> (now - entry.getValue().cachedAt) > this.cacheTimeMs)
			.map(Map.Entry::getKey)
			.collect(Collectors.toList());
		configsToRemove.forEach(webRootPath -> this.cache.remove(webRootPath));
	}

	public Single<T> computeIfAbsent(String cacheKey, Supplier<Single<T>> supplier, Function<Throwable, T> fallback) {
		kickOld();
		CacheEntry<T> entry = this.cache.computeIfAbsent(cacheKey, path -> {
			CacheEntry<T> newEntry = new CacheEntry<T>();
			newEntry.cachedAt = System.currentTimeMillis();
			newEntry.configNode = ReplaySubject.createWithSize(1);
			supplier.get().toObservable()
				.subscribe(newEntry.configNode);
			return newEntry;
		});
		return entry.configNode
			// make sure entries containing errors are removed from the cache
			.doOnError(err -> this.cache.remove(cacheKey))
			.singleOrError()
			.onErrorReturn(fallback::apply);
	}

	private class CacheEntry<U> {
		Long cachedAt;
		ReplaySubject<U> configNode;
	}
}
