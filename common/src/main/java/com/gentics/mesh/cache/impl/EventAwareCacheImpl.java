package com.gentics.mesh.cache.impl;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.gentics.mesh.cache.EventAwareCache;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventBusStore;
import com.gentics.mesh.metric.CachingMetric;
import com.gentics.mesh.metric.MetricsService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Weigher;

import io.micrometer.core.instrument.Counter;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Predicate;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see EventAwareCache
 */
public class EventAwareCacheImpl<K, V> implements EventAwareCache<K, V> {

	private static final Logger log = LoggerFactory.getLogger(EventAwareCacheImpl.class);

	private final Cache<K, Optional<V>> cache;

	private final MeshOptions options;

	private final Predicate<Message<JsonObject>> filter;

	private BiConsumer<Message<JsonObject>, EventAwareCache<K, V>> onNext;

	private boolean disabled = false;

	private final Counter invalidateKeyCounter;
	private final Counter invalidateAllCounter;
	private final Counter missCounter;
	private final Counter hitCounter;
	private final long maxSize;

	private Disposable eventSubscription;


	public EventAwareCacheImpl(String name, long maxSize, Duration expireAfter, Duration expireAfterAccess, EventBusStore eventBusStore, MeshOptions options, MetricsService metricsService,
							   Predicate<Message<JsonObject>> filter,
							   BiConsumer<Message<JsonObject>, EventAwareCache<K, V>> onNext, MeshEvent... events) {
		this(name, maxSize, expireAfter, expireAfterAccess, eventBusStore, options, metricsService, filter, onNext, Optional.empty(), events);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public EventAwareCacheImpl(String name, long maxSize, Duration expireAfter, Duration expireAfterAccess, EventBusStore eventBusStore, MeshOptions options, MetricsService metricsService,
							   Predicate<Message<JsonObject>> filter,
							   BiConsumer<Message<JsonObject>, EventAwareCache<K, V>> onNext, Optional<Weigher<K, Optional<V>>> maybeWeigher, MeshEvent... events) {
		this.options = options;
		this.maxSize = maxSize;
		Caffeine<K, Optional<V>> cacheBuilder = maybeWeigher.map(weigher -> Caffeine.newBuilder().maximumWeight(maxSize).weigher(weigher)).orElseGet(() -> (Caffeine) Caffeine.newBuilder().maximumSize(maxSize));
		if (expireAfter != null) {
			cacheBuilder = cacheBuilder.expireAfterWrite(expireAfter.getSeconds(), TimeUnit.SECONDS);
		}
		if (expireAfterAccess != null) {
			cacheBuilder = cacheBuilder.expireAfterAccess(expireAfterAccess.getSeconds(), TimeUnit.SECONDS);
		}
		this.cache = cacheBuilder.build();
		this.filter = filter;
		this.onNext = onNext;
		registerEventHandlers(eventBusStore, events);
		invalidateKeyCounter = metricsService.counter(new CachingMetric(CachingMetric.Event.CLEAR_SINGLE, name));
		invalidateAllCounter = metricsService.counter(new CachingMetric(CachingMetric.Event.CLEAR_ALL, name));
		missCounter = metricsService.counter(new CachingMetric(CachingMetric.Event.MISS, name));
		hitCounter = metricsService.counter(new CachingMetric(CachingMetric.Event.HIT, name));
	}

	private void registerEventHandlers(EventBusStore eventBusStore, MeshEvent... events) {
		eventBusStore.eventBus().subscribe((eb) -> {
			if (log.isTraceEnabled()) {
				log.trace("Registering to events");
			}
			Observable<Message<JsonObject>> o = rxEventBus(eb, events);
			if (filter != null) {
				o = o.filter(filter);
			}

			// Dispose previous event bus subscription
			if (eventSubscription != null && !eventSubscription.isDisposed()) {
				eventSubscription.dispose();
			}

			eventSubscription = o.subscribe(event -> {
				// Use a default implementation which will invalidate the whole cache on every event
				if (log.isTraceEnabled()) {
					log.trace("Got event: {}", event.body());
				}
				if (onNext == null) {
					invalidate();
				} else {
					onNext.accept(event, this);
				}
			}, error -> {
				log.error("Error while handling event in cache. Disabling cache.", error);
				disable();
			});
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
	public long used() {
		return cache.policy().eviction().flatMap(ev -> ev.weightedSize().stream().mapToObj(Long::valueOf).findAny()).orElseGet(() -> cache.estimatedSize());
	}

	@Override
	public long capacity() {
		return cache.policy().eviction().map(ev -> ev.getMaximum()).orElse(maxSize);
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
		cache.put(key, Optional.ofNullable(value));
	}

	@Override
	public V get(K key) {
		if (disabled) {
			return null;
		}
		if (options.getMonitoringOptions().isEnabled()) {
			@Nullable Optional<V> value = cache.getIfPresent(key);
			if (value == null) {
				missCounter.increment();
				return null;
			} else {
				hitCounter.increment();
			}
			return value.orElse(null);
		} else {
			@Nullable Optional<V> value = cache.getIfPresent(key);
			if (value == null) {
				return null;
			}
			return value.orElse(null);
		}
	}

	@Override
	public V get(K key, Function<? super K, ? extends V> mappingFunction) {
		if (disabled) {
			return mappingFunction.apply(key);
		}
		if (options.getMonitoringOptions().isEnabled()) {
			AtomicBoolean wasCached = new AtomicBoolean(true);
			@Nullable Optional<V> value = cache.getIfPresent(key);
			if (value == null) {
				wasCached.set(false);
				value = Optional.ofNullable(mappingFunction.apply(key));
				if (value != null) {
					cache.put(key, value);
				}
			}
			if (wasCached.get()) {
				hitCounter.increment();
			} else {
				missCounter.increment();
			}
			return value.orElse(null);
		} else {
			@Nullable Optional<V> value = cache.getIfPresent(key);
			if (value == null) {
				value = Optional.ofNullable(mappingFunction.apply(key));
				if (value != null) {
					cache.put(key, value);
				}
			}
			return value.orElse(null);
		}
	}

	/**
	 * Builder for caches.
	 * 
	 * @param <K>
	 * @param <V>
	 */
	public static class Builder<K, V> {
		private boolean disabled = false;

		private long maxSize = 1000;
		private Predicate<Message<JsonObject>> filter = null;
		private BiConsumer<Message<JsonObject>, EventAwareCache<K, V>> onNext = null;
		private MeshEvent[] events = null;
		private EventBusStore eventBusStore;
		private Duration expireAfter;
		private Duration expireAfterAccess;
		private String name;
		private MeshOptions options;
		private MetricsService metricsService;
		private Optional<Weigher<K, Optional<V>>> maybeWeigher = Optional.empty();

		/**
		 * Build the cache instance.
		 * 
		 * @return Created instance
		 */
		public EventAwareCache<K, V> build() {
			Objects.requireNonNull(events, "No events for the cache have been set");
			Objects.requireNonNull(name, "No name has been set");
			EventAwareCacheImpl<K, V> c = new EventAwareCacheImpl<>(name, maxSize, expireAfter, expireAfterAccess, eventBusStore, options, metricsService, filter, onNext, maybeWeigher, events);
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
		 * Sets the event bus store
		 *
		 * @param eventBusStore
		 * @return Fluent API
		 */
		public Builder<K, V> eventBusStore(EventBusStore eventBusStore) {
			this.eventBusStore = eventBusStore;
			return this;
		}

		/**
		 * Sets the mesh options which will be used to determine if cache metrics are enabled.
		 * 
		 * @param options
		 * @return
		 */
		public Builder<K, V> meshOptions(MeshOptions options) {
			this.options = options;
			return this;
		}

		/**
		 * Set the metrics service which will be used to track caching statistics.
		 * 
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
		 * 
		 * @param name
		 * @return Fluent API
		 */
		public Builder<K, V> name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Set the item weigher function
		 * 
		 * @param maybeWeigher
		 * @return Fluent API
		 */
		public Builder<K, V> setWeigher(Weigher<K, Optional<V>> weigher) {
			this.maybeWeigher = Optional.ofNullable(weigher);
			return this;
		}
	}

	/**
	 * Return an observable which emits eventbus messages for the given addresses.
	 * 
	 * @param eventBus
	 *            Eventbus used for registration
	 * @param addresses
	 *            Addresses to listen to
	 * @return
	 */
	public static Observable<Message<JsonObject>> rxEventBus(EventBus eventBus, MeshEvent... addresses) {
		return Observable.fromArray(addresses)
			.flatMap(meshEvent -> Observable.using(
				() -> eventBus.<JsonObject>consumer(meshEvent.address),
				consumer -> Observable.create(sub -> consumer.handler(sub::onNext)),
				MessageConsumer::unregister));
	}
}
