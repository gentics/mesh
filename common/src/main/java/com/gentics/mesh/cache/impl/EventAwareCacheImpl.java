package com.gentics.mesh.cache.impl;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.gentics.mesh.cache.EventAwareCache;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.metric.CachingMetric;
import com.gentics.mesh.metric.MetricsService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.micrometer.core.instrument.Counter;
import io.reactivex.Observable;
import io.reactivex.functions.Predicate;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class EventAwareCacheImpl<K, V> implements EventAwareCache<K, V> {

	private static final Logger log = LoggerFactory.getLogger(EventAwareCacheImpl.class);

	private final Cache<K, V> cache;

	private final Vertx vertx;

	private final MeshOptions options;

	private final Predicate<Message<JsonObject>> filter;

	private BiConsumer<Message<JsonObject>, EventAwareCache<K, V>> onNext;

	private boolean disabled = false;

	private final Counter invalidateKeyCounter;
	private final Counter invalidateAllCounter;
	private final Counter missCounter;
	private final Counter hitCounter;

	public EventAwareCacheImpl(String name, long maxSize, Duration expireAfter, Duration expireAfterAccess, Vertx vertx, MeshOptions options, MetricsService metricsService,
							   Predicate<Message<JsonObject>> filter,
							   BiConsumer<Message<JsonObject>, EventAwareCache<K, V>> onNext, MeshEvent... events) {
		this.vertx = vertx;
		this.options = options;
		Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder().maximumSize(maxSize);
		if (expireAfter != null) {
			cacheBuilder = cacheBuilder.expireAfterWrite(expireAfter.getSeconds(), TimeUnit.SECONDS);
		}
		if (expireAfterAccess != null) {
			cacheBuilder = cacheBuilder.expireAfterAccess(expireAfterAccess.getSeconds(), TimeUnit.SECONDS);
		}
		this.cache = cacheBuilder.build();
		this.filter = filter;
		this.onNext = onNext;
		registerEventHandlers(events);
		invalidateKeyCounter = metricsService.counter(new CachingMetric(CachingMetric.Event.CLEAR_SINGLE, name));
		invalidateAllCounter = metricsService.counter(new CachingMetric(CachingMetric.Event.CLEAR_ALL, name));
		missCounter = metricsService.counter(new CachingMetric(CachingMetric.Event.MISS, name));
		hitCounter = metricsService.counter(new CachingMetric(CachingMetric.Event.HIT, name));
	}

	private void registerEventHandlers(MeshEvent... events) {
		if (log.isTraceEnabled()) {
			log.trace("Registering to events");
		}
		EventBus eb = vertx.eventBus();
		Observable<Message<JsonObject>> o = rxEventBus(eb, events);
		if (filter != null) {
			o = o.filter(filter);
		}

		o.subscribe(event -> {
			// Use a default implementation which will invalidate the whole cache on every event
			if (onNext == null) {
				invalidate();
			} else {
				onNext.accept(event, this);
			}
		}, error -> {
			log.error("Error while handling event in cache. Disabling cache.", error);
			disable();
		});
	}

	@Override
	public void disable() {
		disabled = true;
	}

	@Override
	public void enable() {
		disabled = false;
	}

	@Override
	public long size() {
		cache.cleanUp();
		return cache.estimatedSize();
	}

	@Override
	public void invalidate() {
		if (log.isTraceEnabled()) {
			log.trace("Invalidating full cache");
		}
		if (options.getMonitoringOptions().isEnabled()) {
			invalidateAllCounter.increment();
		}
		cache.invalidateAll();
	}

	@Override
	public void invalidate(K key) {
		if (log.isTraceEnabled()) {
			log.trace("Invalidating entry with key {" + key + "}");
		}
		if (options.getMonitoringOptions().isEnabled()) {
			invalidateKeyCounter.increment();
		}
		cache.invalidate(key);
	}

	@Override
	public void put(K key, V value) {
		if (disabled) {
			return;
		}
		cache.put(key, value);
	}

	@Override
	public V get(K key) {
		if (disabled) {
			return null;
		}
		if (options.getMonitoringOptions().isEnabled()) {
			V value = cache.getIfPresent(key);
			if (value == null) {
				missCounter.increment();
			} else {
				hitCounter.increment();
			}
			return value;
		} else {
			return cache.getIfPresent(key);
		}
	}

	@Override
	public V get(K key, Function<? super K, ? extends V> mappingFunction) {
		if (disabled) {
			return mappingFunction.apply(key);
		}
		if (options.getMonitoringOptions().isEnabled()) {
			AtomicBoolean wasCached = new AtomicBoolean(true);
			V value = cache.get(key, k -> {
				wasCached.set(false);
				return mappingFunction.apply(k);
			});
			if (wasCached.get()) {
				hitCounter.increment();
			} else {
				missCounter.increment();
			}
			return value;
		} else {
			return cache.get(key, mappingFunction);
		}
	}

	public static class Builder<K, V> {
		private boolean disabled = false;

		private long maxSize = 1000;
		private Predicate<Message<JsonObject>> filter = null;
		private BiConsumer<Message<JsonObject>, EventAwareCache<K, V>> onNext = null;
		private MeshEvent[] events = null;
		private Vertx vertx;
		private Duration expireAfter;
		private Duration expireAfterAccess;
		private String name;
		private MeshOptions options;
		private MetricsService metricsService;

		public EventAwareCache<K, V> build() {
			Objects.requireNonNull(events, "No events for the cache have been set");
			Objects.requireNonNull(vertx, "No Vert.x instance has been set");
			Objects.requireNonNull(name, "No name has been set");
			EventAwareCacheImpl<K, V> c = new EventAwareCacheImpl<>(name, maxSize, expireAfter, expireAfterAccess, vertx, options, metricsService, filter, onNext, events);
			if (disabled) {
				c.disable();
			}
			return c;
		}

		/**
		 * Set the events to react upon.
		 * 
		 * @param events
		 * @return Fluent API
		 */
		public Builder<K, V> events(MeshEvent... events) {
			this.events = events;
			return this;
		}

		/**
		 * Set the event filter.
		 * 
		 * @param filter
		 * @return Fluent API
		 */
		public Builder<K, V> filter(Predicate<Message<JsonObject>> filter) {
			this.filter = filter;
			return this;
		}

		/**
		 * Action which will be invoked on every received event.
		 * 
		 * @return Fluent API
		 */
		public Builder<K, V> action(BiConsumer<Message<JsonObject>, EventAwareCache<K, V>> onNext) {
			this.onNext = onNext;
			return this;
		}

		/**
		 * Disable the created cache.
		 * 
		 * @return Fluent API
		 */
		public Builder<K, V> disabled() {
			this.disabled = true;
			return this;
		}

		/**
		 * Set the vertx instance to be used for eventbus communication.
		 * 
		 * @param vertx
		 * @return Fluent API
		 */
		public Builder<K, V> vertx(Vertx vertx) {
			this.vertx = vertx;
			return this;
		}

		/**
		 * Sets the mesh options which will be used to determine if cache metrics are enabled.
		 * @param options
		 * @return
		 */
		public Builder<K, V> meshOptions(MeshOptions options) {
			this.options = options;
			return this;
		}

		/**
		 * Set the metrics service which will be used to track caching statistics.
		 * @param metricsService
		 * @return
		 */
		public Builder<K, V> setMetricsService(MetricsService metricsService) {
			this.metricsService = metricsService;
			return this;
		}

		/**
		 * Set the maximum size for the cache.
		 * 
		 * @param maxSize
		 * @return Fluent API
		 */
		public Builder<K, V> maxSize(long maxSize) {
			this.maxSize = maxSize;
			return this;
		}

		/**
		 * Define when the cache should automatically expire.
		 * 
		 * @param amount
		 * @param unit
		 * @return Fluent API
		 */
		public Builder<K, V> expireAfter(long amount, TemporalUnit unit) {
			this.expireAfter = Duration.of(amount, unit);
			return this;
		}

		/**
		 * Define when the cache should automatically expire after last access
		 * 
		 * @param amount
		 * @param unit
		 * @return Fluent API
		 */
		public Builder<K, V> expireAfterAccess(long amount, TemporalUnit unit) {
			this.expireAfterAccess = Duration.of(amount, unit);
			return this;
		}

		/**
		 * Sets the name for the cache. This is used for caching metrics.
		 * @param name
		 * @return Fluent API
		 */
		public Builder<K, V> name(String name) {
			this.name = name;
			return this;
		}
	}

	public static Observable<Message<JsonObject>> rxEventBus(EventBus eventBus, MeshEvent... addresses) {
		return Observable.fromArray(addresses)
			.flatMap(meshEvent -> Observable.using(
				() -> eventBus.<JsonObject>consumer(meshEvent.address),
				consumer -> Observable.create(sub -> consumer.handler(sub::onNext)),
				MessageConsumer::unregister));
	}
}
