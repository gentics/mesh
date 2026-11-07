package com.gentics.mesh.plugin.like.service;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.like.config.LikePluginConfig;

import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CachingService
 */
@Singleton
public class CachingService {
	private final static String INVALIDATE_ADDRESS = "mesh.plugins.like.invalidate";

	private static final Logger log = LoggerFactory.getLogger(CachingService.class);

	private final LikePluginConfig configuration;

	private final Vertx vertx;

	private static Map<String, Object> cache = new HashMap<>();

	@Inject
	public CachingService(LikePluginConfig configuration, Vertx vertx) {
		this.configuration = configuration;
		this.vertx = vertx;
		this.vertx.eventBus().consumer(INVALIDATE_ADDRESS, (Message<String> msg) -> {
			cache.remove(msg.body());
		});
	}

	/**
	 * Get a cached instance, or create the instance and put into the cache (if caching is activated)
	 * @param key cache key
	 * @param create function that creates the cached object
	 * @return object
	 */
	@SuppressWarnings("unchecked")
	public <T> Single<T> get(String key, Function<String, Single<T>> create) {
		try {
			if (configuration.isCaching()) {
				T cached = null;
				Object cachedValue = cache.get(key);
				if (cachedValue != null) {
					try {
						cached = (T) cachedValue;
					} catch (ClassCastException e) {
						log.warn(String.format("Object cached for key %s was of unexpected type %s", key, cached.getClass().getName()));
					}
				}
				if (cached == null) {
					return create.apply(key).doOnSuccess(value -> {
						cache.put(key, value);
					});
				} else {
					return Single.just(cached);
				}
			} else {
				return create.apply(key);
			}
		} catch (Exception e) {
			return Single.error(e);
		}
	}

	/**
	 * Invalidate the cache for the given key
	 * @param key cache key
	 */
	public void invalidate(String key) {
//		cache.remove(key);
		vertx.eventBus().publish(INVALIDATE_ADDRESS, key);
	}
}
