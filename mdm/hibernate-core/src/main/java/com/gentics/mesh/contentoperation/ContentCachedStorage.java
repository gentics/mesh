package com.gentics.mesh.contentoperation;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.cache.CacheStatus;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.field.HibHtmlField;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoBufferEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoUtil;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.database.HibernateDatabase;
import com.gentics.mesh.etc.config.ConfigUtils;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.hibernate.HibernateCacheConfig;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;
import com.gentics.mesh.hibernate.util.StringScale;
import com.gentics.mesh.metric.MetricsService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;

import dagger.Lazy;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import io.reactivex.Flowable;

/**
 * Implements a cached data storage for content. When running in standalone mode, the cache is backed by caffeine.
 * When running in clustered mode, we are using a distributed cache provided by hazelcast.
 */
@Singleton
public class ContentCachedStorage implements DebugInfoProvider {
	/*
	 * Default content weight in bytes:
	 * 	DB_UUID = 16
	 *  DB_VERSION = 8,
	 *  EDITOR_DB_UUID = 16,
	 *  EDITED = 8,
	 *  BUCKET_ID = 4,
	 *  SCHEMA_DB_UUID = 16,
	 *  SCHEMA_VERSION_DB_UUID = 16,
	 *  LANGUAGE_TAG = vary,
	 *  NODE = 16,
	 *  CURRENT_VERSION_NUMBER = vary, up to 6
	 *  2 as a bias, containing JVM related class entity data
	 * */
	private static final int COMMON_FIELD_WEIGHT = 16+8+16+8+4+16+16+16+2;
	
	public static final int DEFAULT_PERCENT_OF_XMX = 70;

	private static final String CONTENT_MAP = "content";

	protected static final Logger log = LoggerFactory.getLogger(ContentCachedStorage.class);

	private final Lazy<HazelcastInstance> hazelcast;
	private final ContentNoCacheStorage storage;
	private final boolean isClustered;
	private final HibernateMeshOptions options;

	private Optional<LoadingCache<ContentKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?>>> caffeineCache = Optional.empty();
	private long maxCacheSize;
	private final boolean isWeight;

	@Inject
	public ContentCachedStorage(Lazy<HazelcastInstance> hazelcast, ContentNoCacheStorage storage, HibernateMeshOptions options) {
		this.hazelcast = hazelcast;
		this.storage = storage;
		this.isClustered = options.getClusterOptions().isEnabled();
		this.options = options;
		String cacheSizeRaw = ((HibernateCacheConfig) options.getCacheConfig()).getFieldContainerCacheSize();
		if (StringUtils.isNotBlank(cacheSizeRaw)) {
			cacheSizeRaw = cacheSizeRaw.replace("_", "");
			Matcher percentageMatcher = ConfigUtils.QUOTA_PATTERN_PERCENTAGE.matcher(cacheSizeRaw);
			Matcher sizeMatcher = ConfigUtils.QUOTA_PATTERN_SIZE.matcher(cacheSizeRaw);
			Matcher numberMatcher = ConfigUtils.QUOTA_PATTERN_NUMBER.matcher(cacheSizeRaw);
			if (percentageMatcher.matches()) {
				this.isWeight = true;
				this.maxCacheSize = Runtime.getRuntime().maxMemory() / 100L * Long.parseLong(percentageMatcher.group("value"));
			} else if (sizeMatcher.matches()) {
				this.isWeight = true;
				this.maxCacheSize = ConfigUtils.getBytes(sizeMatcher);
			} else if (numberMatcher.matches()) {
				this.isWeight = false;
				this.maxCacheSize = Long.parseLong(numberMatcher.group("value"));
			} else {
				log.warn("Invalid field container cache size value `{}`. Caching is disabled.", cacheSizeRaw);
				this.maxCacheSize = 0;
				this.isWeight = false;
			}
		} else {
			this.maxCacheSize = 0;
			this.isWeight = false;
		}
	}

	/**
	 * Check if the content caching is enabled
	 * 
	 * @return
	 */
	public boolean isContentCachingEnabled() {
		return this.maxCacheSize > 0;
	}

	/**
	 * Initialize the cache
	 * @param db
	 * @param statisticsEnabled whether we should add cache metrics to the metrics registry
	 */
	public void init(HibernateDatabase db, boolean statisticsEnabled) {
		loadCaffeineCache(statisticsEnabled);
		if (isClustered) {
			loadHazelcastCache(db, statisticsEnabled);
		}
	}

	private void loadHazelcastCache(HibernateDatabase db, boolean statisticsEnabled) {
		Config config = null;
		String hazelcastFilePath = new File("").getAbsolutePath() + File.separator + "config" + File.separator + "hazelcast.xml";
		try {
			config = new XmlConfigBuilder(hazelcastFilePath).build();
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Please provide a " + hazelcastFilePath + " file", e);
		}
		config.setInstanceName(options.getClusterOptions().getClusterName());
		config.setClusterName(options.getClusterOptions().getClusterName());
		config.setClassLoader(Thread.currentThread().getContextClassLoader());

		Hazelcast.getOrCreateHazelcastInstance(config);

		ITopic<ContentKey> evictTopic = hazelcast.get().getTopic(CONTENT_MAP);
		evictTopic.addMessageListener(message -> {
			if (!message.getPublishingMember().localMember()) {
				ContentKey key = message.getMessageObject();
				if (key != null) {
					if (log.isDebugEnabled()) {
						log.debug("Evicting object with key {} from cache", key);
					}
					evictOne(key);
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Evicting all objects from cache");
					}
					reset();
				}
			}
		});
	}

	private void reset() {
		log.debug("Reset the cache");
		caffeineCache.ifPresent(Cache::invalidateAll);
	}

	private void evictOne(ContentKey key) {
		log.debug("Evict one cache item {}", key);
		caffeineCache.ifPresent(c -> c.invalidate(key));
	}

	private void loadCaffeineCache(boolean statisticsEnabled) {
		Caffeine<Object, Object> caffeineBuilder = Caffeine.newBuilder();

		if (isWeight) {
			caffeineBuilder.maximumWeight(maxCacheSize).weigher(this::weigh);
		} else {
			caffeineBuilder.maximumSize(maxCacheSize);
		}
		if (statisticsEnabled) {
			caffeineBuilder.recordStats();
		}

		caffeineCache = Optional.of(caffeineBuilder.build(new CacheLoader<>() {
			@CheckForNull
			@Override
			public HibUnmanagedFieldContainer<?, ?, ?, ?, ?> load(@Nonnull ContentKey contentKey) throws Exception {
				return storage.findOne(contentKey);
			}

			@SuppressWarnings("unchecked")
			@Nonnull
			@Override
			public Map<ContentKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> loadAll(@Nonnull Iterable<? extends ContentKey> keys) throws Exception {
				return storage.findMany((Collection<ContentKey>) keys);
			}
		}));
	}

	/**
	 * Load the single key from the cache. If not found in the cache, it will be loaded from disk.
	 * @param key
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public HibUnmanagedFieldContainer<?, ?, ?, ?, ?> findOne(ContentKey key) {
		return caffeineCache.map(c -> c.get(key)).orElseGet(() -> (HibUnmanagedFieldContainer) storage.findOne(key));
	}

	/**
	 * Find many keys from the cache. If not found on the cache, they will be loaded from disk.
	 * @param keys
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> List<T> findMany(Set<ContentKey> keys) {
		return new ArrayList<T>((Collection<? extends T>) caffeineCache.map(c -> c.getAll(keys)).orElseGet(() -> storage.findMany(keys)).values());
	}

	/**
	 * Find many keys from the cache. If not found on the cache, they will be loaded from disk.
	 * @param keys
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> Stream<T> findManyStream(Set<ContentKey> keys) {
		return (Stream<T>) caffeineCache.map(c -> c.getAll(keys).values()).orElse(Collections.emptyList()).stream();
	}

	/**
	 * Evict all keys from cache.
	 */
	public void evictAll() {
		if (log.isDebugEnabled()) {
			log.debug("Evicting all objects from cache");
		}
		reset();
		if (isClustered) {
			ITopic<ContentKey> evictTopic = hazelcast.get().getTopic(CONTENT_MAP);
			evictTopic.publish(null);
		}
	}

	/**
	 * Evict given key from cache.
	 * @param key
	 */
	public void evict(ContentKey key) {
		if (log.isDebugEnabled()) {
			log.debug("Evicting object with key {} from cache", key);
		}
		evictOne(key);
		if (isClustered) {
			ITopic<ContentKey> evictTopic = hazelcast.get().getTopic(CONTENT_MAP);
			evictTopic.publish(key);
		}
	}

	/**
	 * Register cache query metrics to the metrics registry
	 * @param metrics
	 */
	public void registerMetrics(MetricsService metrics) {
		caffeineCache.ifPresent(c -> CaffeineCacheMetrics.monitor(metrics.getMetricRegistry(), c, CONTENT_MAP));
	}

	/**
	 * Return all the elements that are currently stored in the cache
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> Map<ContentKey, T> getAllInCache() {
		return caffeineCache.map(c -> (Map<ContentKey, T>) c.asMap()).orElse(Collections.emptyMap());
	}

	private @NonNegative int weigh(@NonNull ContentKey key,
			@NonNull HibUnmanagedFieldContainer<?, ?, ?, ?, ?> value) {
		HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version = value.getSchemaContainerVersion();
		int w = version.getSchema().getFields().stream().map(field -> {
			FieldTypes type = FieldTypes.valueByName(field.getType());
			int languageTagSize = Optional.ofNullable(value.getLanguageTag()).map(StringScale::getWeight).orElse(0);
			int elementVersionSize = Optional.ofNullable(value.getElementVersion()).map(StringScale::getWeight).orElse(0);
			int weightOfType = weightOfType(field, type, value);
			return COMMON_FIELD_WEIGHT + languageTagSize + elementVersionSize + weightOfType;
		}).reduce(1, Integer::sum);
		if (log.isDebugEnabled()) {
			long currentSize = getCurrentCacheSizeInUnits();
			log.debug("Cached {} bytes of {}, total is {} of max {}", w, key, FileUtils.byteCountToDisplaySize(currentSize), FileUtils.byteCountToDisplaySize(getMaxCacheSizeInUnits()));
		}
		return w;
	}

	private long getCurrentCacheSizeInUnits() {
		return caffeineCache.map(c -> c.policy().eviction().flatMap(ev -> ev.weightedSize().stream().mapToObj(Long::valueOf).findAny()).orElseGet(() -> c.estimatedSize())).orElse(0L);
	}

	public long getMaxCacheSizeInUnits() {
		return caffeineCache.map(c -> c.policy().eviction().map(ev -> ev.getMaximum()).orElse(maxCacheSize)).orElse(0L);
	}

	private int weightOfType(FieldSchema field, FieldTypes type, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> container) {
		switch (type) {
		case STRING:
			HibStringField sValue = container.getString(field.getName());
			if (sValue != null) {
				String string = sValue.getString();
				if (string != null) {
					return StringScale.getWeight(string);
				}
			}
			return 0;
		case HTML:
			HibHtmlField hValue = container.getHtml(field.getName());
			if (hValue != null) {
				String html = hValue.getHTML();
				if (html != null) {
					return StringScale.getWeight(html);
				}
			}
			return 0;
		case DATE:
		case NUMBER:
			return 8;
		case MICRONODE:
		case NODE:
		case BINARY:
		case BOOLEAN:
		case S3BINARY:
		case LIST:
		default:
			return DebugInfoUtil.POINTER_SIZE;
		}
	}

	@Override
	public String name() {
		return "contentCache";
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		CacheStatus status = getStatus();
		return Flowable.just(DebugInfoBufferEntry.fromString(name() + ".json", status.toJson()));
	}

	/**
	 * Get current cache status.
	 * 
	 * @return
	 */
	public CacheStatus getStatus() {
		return new CacheStatus(name(), getCurrentCacheSizeInUnits(), getMaxCacheSizeInUnits(), options.getCacheConfig().getFieldContainerCacheSize());
	}
}
