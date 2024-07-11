package com.gentics.mesh.cache;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.cache.impl.EventAwareCacheImpl.Builder;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoBufferEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoUtil;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.hibernate.HibernateCacheConfig;
import com.gentics.mesh.etc.config.ConfigUtils;
import com.gentics.mesh.hibernate.data.domain.AbstractHibListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibDateListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibHtmlListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNumberListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibStringListFieldEdgeImpl;
import com.gentics.mesh.hibernate.util.StringScale;

import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ListableFieldCache}
 */
@Singleton
public class ListableFieldCacheImpl extends AbstractMeshCache<UUID, List<? extends AbstractHibListFieldEdgeImpl<?>>> 
			implements ListableFieldCache<AbstractHibListFieldEdgeImpl<?>>, DebugInfoProvider {

	public static final int DEFAULT_PERCENT_OF_XMX = 20;

	protected static final Logger log = LoggerFactory.getLogger(ListableFieldCacheImpl.class);

	protected final HibernateCacheConfig config;

	/**
	 * Events which will clear the cache
	 */
	private static final MeshEvent EVENTS[] = {
		MeshEvent.NODE_CONTENT_CREATED,
		MeshEvent.NODE_CONTENT_DELETED,
		MeshEvent.NODE_UPDATED
	};

	@Inject
	public ListableFieldCacheImpl(EventAwareCacheFactory factory, CacheRegistry registry, HibernateMeshOptions options) {
		super(createCache(factory, options.getCacheConfig()), registry, getCacheSize(options.getCacheConfig()).getKey());
		this.config = options.getCacheConfig();
	}

	@Override
	public void put(UUID listUuid, List<? extends AbstractHibListFieldEdgeImpl<?>> value) {
		cache.put(listUuid, value);
	}

	@Override
	public void invalidate(UUID listUuid) {
		cache.invalidate(listUuid);
	}

	private static int listFieldWeight(UUID uuid, Optional<List<? extends AbstractHibListFieldEdgeImpl<?>>> maybeList) {
		if (maybeList == null || maybeList.isEmpty()) {
			return 0;
		}
		List<? extends AbstractHibListFieldEdgeImpl<?>> list = maybeList.get();
		if (list == null || list.size() < 1) {
			return 0;
		}
		int w;
		if (list.get(0) instanceof HibStringListFieldEdgeImpl) {
			w = list.stream().map(HibStringListFieldEdgeImpl.class::cast).map(HibStringListFieldEdgeImpl::getString).filter(Objects::nonNull).map(StringScale::getWeight).reduce(0, Integer::sum);
		} else if (list.get(0) instanceof HibHtmlListFieldEdgeImpl) {
			w = list.stream().map(HibHtmlListFieldEdgeImpl.class::cast).map(HibHtmlListFieldEdgeImpl::getHTML).filter(Objects::nonNull).map(StringScale::getWeight).reduce(0, Integer::sum);
		} else if (list.get(0) instanceof HibNumberListFieldEdgeImpl || list.get(0) instanceof HibDateListFieldEdgeImpl) {
			w = list.size() * 8;
		} else {
			w = list.size() * DebugInfoUtil.POINTER_SIZE;
		}
		if (log.isDebugEnabled()) {
			log.debug("Cached {} bytes of {}", w, uuid);
		}
		return w;
	}

	/**
	 * Create the cache
	 * @param factory cache factory
	 * @return cache instance
	 */
	private static EventAwareCache<UUID, List<? extends AbstractHibListFieldEdgeImpl<?>>> createCache(EventAwareCacheFactory factory, HibernateCacheConfig cacheConfig) {
		Builder<UUID, List<? extends AbstractHibListFieldEdgeImpl<?>>> builder = factory.<UUID, List<? extends AbstractHibListFieldEdgeImpl<?>>>builder()
			.name("listablefieldvalues")
			.events(EVENTS)
			.action((event, cache) -> {
				cache.invalidate();
			});
		Pair<Long, Boolean> cacheParams = getCacheSize(cacheConfig);
		builder.maxSize(cacheParams.getKey());

		if (cacheParams.getValue()) {
			builder.setWeigher(ListableFieldCacheImpl::listFieldWeight);
		}
		return builder.build();
	}

	@Override
	public String name() {
		return "listableFieldCache";
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		CacheStatusModel status = getStatus();
		return Flowable.just(DebugInfoBufferEntry.fromString(name() + ".json", status.toJson()));
	}

	/**
	 * Get current cache status.
	 * 
	 * @return
	 */
	public CacheStatusModel getStatus() {
		return new CacheStatusModel(name(), cache.used(), cache.capacity(), config.getListFieldCacheSize());
	}

	private static final Pair<Long, Boolean> getCacheSize(HibernateCacheConfig config) {
		String cacheSizeRaw = config.getListFieldCacheSize();
		if (StringUtils.isNotBlank(cacheSizeRaw)) {
			cacheSizeRaw = cacheSizeRaw.replace("_", "");
			Matcher percentageMatcher = ConfigUtils.QUOTA_PATTERN_PERCENTAGE.matcher(cacheSizeRaw);
			Matcher sizeMatcher = ConfigUtils.QUOTA_PATTERN_SIZE.matcher(cacheSizeRaw);
			Matcher numberMatcher = ConfigUtils.QUOTA_PATTERN_NUMBER.matcher(cacheSizeRaw);
			if (percentageMatcher.matches()) {
				return Pair.of(Runtime.getRuntime().maxMemory() / 100L * Long.parseLong(percentageMatcher.group("value")), true);
			} else if (sizeMatcher.matches()) {
				return Pair.of(ConfigUtils.getBytes(sizeMatcher), true);
			} else if (numberMatcher.matches()) {
				return Pair.of(Long.parseLong(numberMatcher.group("value")), false);
			} else {
				log.warn("Invalid list container cache size value `{}`. Caching is disabled.", cacheSizeRaw);
			}
		}
		return Pair.of(0L, false);
	}
}
