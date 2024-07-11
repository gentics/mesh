package com.gentics.mesh.contentoperation;

import static com.gentics.mesh.contentoperation.CommonContentColumn.DB_UUID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.FieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.ContentInterceptor;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * Content access methods implemented on behalf of cache(d/less) storage variant combination.
 *
 * @author plyhun
 *
 */
@Singleton
public class ContentStorageImpl implements ContentStorage {

	private final boolean useCache;
	private final ContentNoCacheStorage nonCachedStorage;
	private ContentCachedStorage cachedStorage;

	@Inject
	public ContentStorageImpl(ContentNoCacheStorage nonCachedStorage, ContentCachedStorage cachedStorage) {
		useCache = cachedStorage != null && cachedStorage.isContentCachingEnabled();
		this.nonCachedStorage = nonCachedStorage;
		this.cachedStorage = cachedStorage;
	}

	@Override
	public HibNodeFieldContainerImpl findOne(FieldSchemaVersionElement<?, ?, ?, ?, ?> version, UUID contentUuid) {
		return findOne(ContentKey.fromContentUUIDAndVersion(contentUuid, version));
	}

	@Override
	public HibMicronodeContainerImpl findOneMicronode(FieldSchemaVersionElement<?, ?, ?, ?, ?> version, UUID contentUuid) {
		return findOne(ContentKey.fromContentUUIDAndVersion(contentUuid, version));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> T findOne(ContentKey key) {
		Optional<T> contentFromInterceptor = findInInterceptor(key);
		if (contentFromInterceptor.isPresent()) {
			return contentFromInterceptor.get();
		}

		if (useCache) {
			return (T) cachedStorage.findOne(key);
		} else {
			return (T) nonCachedStorage.findOne(key);
		}
	}

	@Override
	public List<HibNodeFieldContainerImpl> findMany(FieldSchemaVersionElement<?, ?, ?, ?, ?> version) {
		return findMany(version, HibNodeFieldContainerImpl::new);
	}

	@Override
	public List<HibMicronodeContainerImpl> findManyMicronodes(FieldSchemaVersionElement<?, ?, ?, ?, ?> version) {
		return findMany(version, HibMicronodeContainerImpl::new);
	}

	private <T extends HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> List<T> findMany(FieldSchemaVersionElement<?, ?, ?, ?, ?> version, Supplier<T> constructor) {
		List<UUID> dbUuids = nonCachedStorage.findColumnValues(version, DB_UUID);
		ReferenceType type = version instanceof SchemaVersion ? ReferenceType.FIELD : ReferenceType.MICRONODE;

		if (type.equals(ReferenceType.FIELD)) {
			dbUuids.addAll(contentInterceptor().getFieldContainersUuids());
		} else {
			dbUuids.addAll(contentInterceptor().getMicronodeUuids());
		}

		Set<ContentKey> keys = dbUuids.stream().map(uuid -> new ContentKey(uuid, (UUID) version.getId(), type)).collect(Collectors.toSet());

		return findManyInternal(keys);
	}

	@Override
	public <T extends Comparable<T>> Stream<HibNodeFieldContainerImpl> findMany(List<HibNodeFieldContainerEdgeImpl> edges, Triple<ContentColumn, T, T> columnBetween) {
		Set<ContentKey> keys = edges.stream().map(ContentKey::fromEdge).collect(Collectors.toSet());
		List<HibNodeFieldContainerImpl> containers = findManyInternal(keys);

		return containers.stream()
				.filter(fieldContainer -> isWithinBounds(fieldContainer, columnBetween));
	}

	private <T extends Comparable<T>> boolean isWithinBounds(HibNodeFieldContainerImpl fieldContainer, Triple<ContentColumn, T, T> columnBetween) {
		T column = fieldContainer.get(columnBetween.getLeft(), () -> null);
		T lowerBound = columnBetween.getMiddle();
		T higherBound = columnBetween.getRight();

		return column != null && lowerBound.compareTo(column) <= 0 && higherBound.compareTo(column) >= 0;
	}

	@Override
	public List<HibNodeFieldContainerImpl> findMany(Collection<HibNodeFieldContainerEdgeImpl> edges) {
		Set<ContentKey> keys = edges.stream()
				.map(ContentKey::fromEdge)
				.collect(Collectors.toSet());

		return findManyInternal(keys);
	}

	@Override
	public List<HibMicronodeContainerImpl> findManyMicronodes(Set<ContentKey> keys) {
		return findManyInternal(keys);
	}

	@Override
	public List<HibNodeFieldContainerImpl> findMany(Set<ContentKey> keys) {
		return findManyInternal(keys);
	}

	@SuppressWarnings("unchecked")
	private <T extends HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> List<T> findManyInternal(Set<ContentKey> keys) {
		if (keys.isEmpty()) {
			return Collections.emptyList();
		}
		Map<ContentKey, T> fromInterceptor = keys.stream()
				.map(key -> Pair.of(key, (T) findInInterceptor(key).orElse(null)))
				.filter(pair -> pair.getRight() != null)
				.collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
		List<T> result = new ArrayList<>(fromInterceptor.values());

		// everything was found in the interceptor
		keys.removeAll(fromInterceptor.keySet());
		if (keys.isEmpty()) {
			return result;
		}

		// find all the keys for which no values were found in the interceptor
		if (useCache) {
			result.addAll(cachedStorage.findMany(keys));
		} else {
			Map<ContentKey, T> map = nonCachedStorage.findMany(keys);
			result.addAll(map.values());
		}

		return result;
	}

	@Override
	public <T> T findColumn(FieldSchemaVersionElement<?, ?, ?, ?, ?> version, UUID contentUuid, ContentColumn contentColumn) {
		return nonCachedStorage.findColumn(version, contentUuid, contentColumn);
	}

	@Override
	public void insert(HibNodeFieldContainerImpl container, SchemaVersion schemaVersion) {
		nonCachedStorage.insert(container, schemaVersion);
	}

	@Override
	public void insert(HibMicronodeContainerImpl container, MicroschemaVersion microschemaVersion) {
		nonCachedStorage.insert(container, microschemaVersion);
	}

	@Override
	public void delete(UUID dbUuid, FieldSchemaVersionElement<?, ?, ?, ?, ?> version) {
		if (useCache) {
			ContentKey contentKey = ContentKey.fromContentUUIDAndVersion(dbUuid, version);
			cachedStorage.evict(contentKey);
		}
		nonCachedStorage.delete(dbUuid, version);
	}

	@Override
	public void delete(Collection<? extends HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> fieldContainers) {
		Set<ContentKey> contentKeys = fieldContainers.stream().map(ContentKey::fromContent).collect(Collectors.toSet());
		delete(contentKeys);
	}

	@Override
	public void dropTable(FieldSchemaVersionElement<?, ?, ?, ?, ?> version) {
		if (useCache) {
			cachedStorage.evictAll();
		}
		nonCachedStorage.dropTable(version);
	}

	@Override
	public long getGlobalCount() {
		return nonCachedStorage.getGlobalCount();
	}

	@Override
	public void addColumnIfNotExists(FieldSchemaVersionElement<?, ?, ?, ?, ?> version, DynamicContentColumn column) {
		if (useCache) {
			cachedStorage.evictAll();
		}
		nonCachedStorage.addColumnIfNotExists(version, column);
	}

	@Override
	public void createTable(SchemaVersion version) {
		nonCachedStorage.createTable(version);
	}

	@Override
	public void createIndex(SchemaVersion version, CommonContentColumn column, boolean unique) {
		nonCachedStorage.createIndex(version, column, unique);
	}

	@Override
	public void createMicronodeTable(MicroschemaVersion microVersion) {
		nonCachedStorage.createMicronodeTable(microVersion);
	}

	@Override
	public long delete(FieldSchemaVersionElement<?, ?, ?, ?, ?> version, Project project) {
		if (useCache) {
			Map<ContentKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> inCache = cachedStorage.getAllInCache();
			inCache.entrySet().stream()
					.filter(kv -> {
						HibUnmanagedFieldContainer<?, ?, ?, ?, ?> container = kv.getValue();
						// Since the cache is not transactional, it could contain some field containers that are
						// referencing deleted nodes.
						boolean containerIsOutdated = container.getNode() == null;
						return containerIsOutdated || project.equals(container.getNode().getProject());
					})
					.map(Map.Entry::getKey)
					.forEach(cachedStorage::evict);
		}
		return nonCachedStorage.delete(version, project);
	}

	@Override
	public long delete(SchemaVersion version, Set<HibNodeImpl> nodes) {
		if (useCache) {
			Map<ContentKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> inCache = cachedStorage.getAllInCache();
			inCache.entrySet().stream()
					.filter(kv -> {
						HibUnmanagedFieldContainer<?, ?, ?, ?, ?> container = kv.getValue();
						// Since the cache is not transactional, it could contain some field containers that are
						// referencing deleted nodes.
						boolean containerIsOutdated = container.getNode() == null;
						return containerIsOutdated || nodes.contains(container.getNode());
					})
					.map(Map.Entry::getKey)
					.forEach(cachedStorage::evict);
		}

		return nonCachedStorage.delete(version, nodes);
	}

	@Override
	public long delete(Set<ContentKey> contentKeys) {
		if (useCache) {
			contentKeys.forEach(cachedStorage::evict);
		}

		return nonCachedStorage.delete(contentKeys);
	}

	@Override
	public long deleteUnreferencedMicronodes(MicroschemaVersion version) {
		if (useCache) {
			// since identifying the unreferenced micronodes is not straightforward (requires extra queries),
			// we simple delete all micronodes from the cache.
			Map<ContentKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> inCache = cachedStorage.getAllInCache();
			inCache.keySet().stream()
					.filter(fieldContainer -> fieldContainer.getType().equals(ReferenceType.MICRONODE))
					.forEach(cachedStorage::evict);
		}

		return nonCachedStorage.deleteUnreferencedMicronodes(version);
	}

	private ContentInterceptor contentInterceptor() {
		return HibernateTx.get().getContentInterceptor();
	}

	@SuppressWarnings("unchecked")
	private <T extends HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> Optional<T> findInInterceptor(ContentKey key) {
		Optional<T> contentFromInterceptor = key.getType().equals(ReferenceType.FIELD) ?
				(Optional<T>) contentInterceptor().get(key.getContentUuid()) :
				(Optional<T>) contentInterceptor().getMicronode(key.getContentUuid());

		return contentFromInterceptor;
	}

	@Override
	public List<ContentKey> findByNodes(SchemaVersion version, Set<HibNodeImpl> nodes) {
		return nonCachedStorage.findByNodes(version, nodes);
	}
}
