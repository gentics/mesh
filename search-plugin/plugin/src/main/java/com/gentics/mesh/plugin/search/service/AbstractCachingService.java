package com.gentics.mesh.plugin.search.service;

import com.gentics.mesh.plugin.search.config.SearchConfiguration;
import com.gentics.mesh.plugin.search.util.RxUtils;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Base class for plugin services which cache their provided data.
 *
 * <p>
 *     Derived classes should only use {@link #getFromCache(Object)} to load
 *     values to ensure that the {@link SearchConfiguration#isCaching()} setting
 *     is heeded.
 * </p>
 *
 * @param <K> The cache key type
 * @param <V> The type of the cached values
 */
public abstract class AbstractCachingService<K, V> {

	protected final Logger log = LoggerFactory.getLogger(getClass());
	protected final SearchConfiguration config;

	private final AsyncLoadingCache<K, V> cache;

	AbstractCachingService(SearchConfiguration config) {
		this.config = config;
		this.cache = Caffeine.newBuilder()
			.expireAfterWrite(config.getExpireTime(), TimeUnit.SECONDS)
			.buildAsync(this::internalLoadValue);
	}

	/**
	 * Clears the underlying cache.
	 */
	public void clearCache() {
		Cache<?, ?> sync = cache.synchronous();

		if (log.isDebugEnabled()) {
			long count = sync.estimatedSize();

			log.debug(
				"Clearing cache (approximately {} entr{})",
				count,
				count == 1 ? "y" : "ies");
		} else {
			log.info("Clearing cache");
		}

		sync.invalidateAll();
	}

	/**
	 * Method to load data not present in the underlying cache.
	 *
	 * @param key The cache key for the value to load
	 * @return A <code>Single</code> containing the loaded value
	 */
	@Nonnull
	abstract protected Single<V> loadValue(K key);

	/**
	 * Get the value for <code>key</code> from the cache when caching is
	 * {@link SearchConfiguration#isCaching() enabled}, or via
	 * {@link #loadValue(Object)} when it is disabled.
	 *
	 * @param key The cache key for the value to load
	 * @return A <code>Single</code> containing the loaded value
	 */
	protected Single<V> getFromCache(K key) {
		log.debug("Getting value for key {}", key);

		return config.isCaching()
			? Single.fromFuture(cache.get(key))
			: loadValue(key);
	}

	/**
	 * Wrapper for {@link #loadValue(Object)} which can be used as a loader method for
	 * the {@link AsyncLoadingCache}.
	 *
	 * @param key The cache key for the value to load
	 * @param executor The executor to use for loading the value
	 * @return A <code>CompletableFuture</code> containing the loaded value
	 */
	@Nonnull
	private CompletableFuture<V> internalLoadValue(K key, Executor executor) {
		log.debug("No value for key {} found in cache, reloading", key);

		return loadValue(key)
			.subscribeOn(Schedulers.from(executor))
			.to(RxUtils::completableFuture);
	}
}
