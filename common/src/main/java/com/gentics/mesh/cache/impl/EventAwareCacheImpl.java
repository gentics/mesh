package com.gentics.mesh.cache.impl;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.gentics.mesh.cache.EventAwareCache;
import com.gentics.mesh.core.rest.MeshEvent;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

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

	private final Predicate<Message<JsonObject>> filter;

	private BiConsumer<Message<JsonObject>, EventAwareCache<K, V>> onNext;

	private boolean disabled = false;

	public EventAwareCacheImpl(long size, Vertx vertx, Predicate<Message<JsonObject>> filter,
		BiConsumer<Message<JsonObject>, EventAwareCache<K, V>> onNext,
		MeshEvent... events) {
		this.vertx = vertx;
		this.cache = Caffeine.newBuilder().maximumSize(size).build();
		this.filter = filter;
		this.onNext = onNext;
		registerEventHandlers(events);
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
		cache.invalidateAll();
	}

	@Override
	public void invalidate(K key) {
		if (log.isTraceEnabled()) {
			log.trace("Invalidating entry with key {" + key + "}");
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
		return cache.getIfPresent(key);
	}

	@Override
	public V get(K key, Function<? super K, ? extends V> mappingFunction) {
		if (disabled) {
			return mappingFunction.apply(key);
		}
		return cache.get(key, mappingFunction);
	}

	public static <K, V> Builder<K, V> builder() {
		return new Builder<K, V>();
	}

	public static class Builder<K, V> {

		private boolean disabled = false;
		private long size = 1000;
		private Predicate<Message<JsonObject>> filter = null;
		private BiConsumer<Message<JsonObject>, EventAwareCache<K, V>> onNext = null;
		private MeshEvent[] events = null;
		private Vertx vertx;

		public EventAwareCache<K, V> build() {
			Objects.requireNonNull(events, "No events for the cache have been set");
			Objects.requireNonNull(vertx, "No Vert.x instance has been set");
			EventAwareCacheImpl<K, V> c = new EventAwareCacheImpl<>(size, vertx, filter, onNext, events);
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
		 * @param filter
		 * @return Fluent API
		 */
		public Builder<K, V> action(BiConsumer<Message<JsonObject>, EventAwareCache<K, V>> onNext) {
			this.onNext = onNext;
			return this;
		}

		/**
		 * Set the cache size.
		 * 
		 * @param size
		 * @return Fluent API
		 */
		public Builder<K, V> size(long size) {
			this.size = size;
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
		 * Set the vertx instance to be used for eventbus communcation.
		 * 
		 * @param vertx
		 * @return Fluent API
		 */
		public Builder<K, V> vertx(Vertx vertx) {
			this.vertx = vertx;
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
