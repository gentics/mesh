package com.gentics.mesh.auth.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.data.HibNamedBaseElement;
import com.gentics.mesh.core.data.dao.DaoGlobal;

/**
 * Helper class for mapping groups and roles
 *
 * @param <T> type of mapped entitiy
 */
public class MappingHelper <T extends HibNamedBaseElement> {
	/**
	 * Map of handled entities by uuid
	 */
	protected Map<String, T> entitiesByUuid = new HashMap<>();

	/**
	 * Map of handled entities by name
	 */
	protected Map<String, T> entitiesByName = new HashMap<>();

	/**
	 * Set of mapped uuids
	 */
	protected Set<String> mappedUuids = new HashSet<>();

	/**
	 * Set of mapped names
	 */
	protected Set<String> mappedNames = new HashSet<>();

	/**
	 * Set of created uuids
	 */
	protected Set<String> createdUuids = new HashSet<>();

	/**
	 * Dao for the handled entity type
	 */
	protected DaoGlobal<T> dao;

	/**
	 * Create helper instance
	 * @param dao dao
	 */
	public MappingHelper(DaoGlobal<T> dao) {
		this.dao = dao;
	}

	/**
	 * Initialize the entity uuids/names from the given collection. The entities will be treated as "mapped" entities.
	 * @param <U> type of the items in the collection
	 * @param coll collection of items from which either uuid or name can be extracted
	 * @param uuidExtractor extractor for the uuid
	 * @param nameExtractor extractor for the name
	 * @param order order in which uuid/name shall be taken from the collection items
	 */
	public <U> void initMapped(Collection<U> coll, Function<U, String> uuidExtractor, Function<U, String> nameExtractor,
			Order order) {
		if (coll != null) {
			for (U entry : coll) {
				if (entry != null) {
					String uuid = uuidExtractor.apply(entry);
					String name = nameExtractor.apply(entry);
					put(uuid, name, order, true);
				}
			}
		}
	}

	/**
	 * Initialize the entity uuids/names from the given subcollections. The entities will not be treated as "mapped" entities.
	 * @param <U> type of the items in the collection
	 * @param <V> type of the items in the subcollections
	 * @param coll collection of items
	 * @param subCollectionExtractor extractor of subcollections from the collection items
	 * @param uuidExtractor extractor for the uuid
	 * @param nameExtractor extractor for the name
	 * @param order order in which uuid/name shall be taken from the collection items
	 */
	public <U, V> void initAssigned(Collection<U> coll, Function<U, Collection<V>> subCollectionExtractor,
			Function<V, String> uuidExtractor, Function<V, String> nameExtractor, Order order) {
		if (coll != null) {
			for (U entry : coll) {
				if (entry != null) {
					Collection<V> subCollection = subCollectionExtractor.apply(entry);
					if (subCollection != null) {
						for (V subEntry : subCollection) {
							if (subEntry != null) {
								String uuid = uuidExtractor.apply(subEntry);
								String name = nameExtractor.apply(subEntry);
								put(uuid, name, order, false);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Put the uuid or name into the object maps
	 * @param uuid uuid
	 * @param name name
	 * @param order order in which uuid/name shall be considered
	 * @param mapped true if the entity is treated as "mapped"
	 */
	protected void put(String uuid, String name, Order order, boolean mapped) {
		switch (order) {
		case UUID_FIRST:
			if (uuid != null) {
				if (!entitiesByUuid.containsKey(uuid)) {
					entitiesByUuid.put(uuid, null);
				}
			} else if (name != null) {
				if (!entitiesByName.containsKey(name)) {
					entitiesByName.put(name, null);
				}
			}
			break;
		case NAME_FIRST:
			if (name != null) {
				if (!entitiesByName.containsKey(name)) {
					entitiesByName.put(name, null);
				}
			} else if (uuid != null) {
				if (!entitiesByUuid.containsKey(uuid)) {
					entitiesByUuid.put(uuid, null);
				}
			}
			break;
		}

		if (mapped) {
			if (uuid != null) {
				mappedUuids.add(uuid);
			}
			if (name != null) {
				mappedNames.add(name);
			}
		}
	}

	/**
	 * Load the initialized entities
	 */
	public void load() {
		Set<String> uuidsToLoad = entitiesByUuid.entrySet().stream().filter(entry -> entry.getValue() == null)
				.map(entry -> entry.getKey()).collect(Collectors.toSet());
		dao.findByUuids(uuidsToLoad).forEach(pair -> {
			T entity = pair.getRight();
			if (entity != null) {
				entitiesByUuid.put(entity.getUuid(), entity);
				entitiesByName.put(entity.getName(), entity);
			}
		});
		Set<String> namesToLoad = entitiesByName.entrySet().stream().filter(entry -> entry.getValue() == null)
				.map(entry -> entry.getKey()).collect(Collectors.toSet());

		dao.findByNames(namesToLoad).forEach(pair -> {
			T entity = pair.getRight();
			if (entity != null) {
				entitiesByUuid.put(entity.getUuid(), entity);
				entitiesByName.put(entity.getName(), entity);
			}
		});
	}

	/**
	 * Check whether any entities, which were initialized as "mapped" could not be loaded via {@link #load()}
	 * @return true if "mapped" entities are missing
	 */
	public boolean areMappedEntitiesMissing() {
		return entitiesByName.entrySet().stream().filter(entry -> mappedNames.contains(entry.getKey()))
				.anyMatch(entry -> entry.getValue() == null);
	}

	/**
	 * Create the missing "mapped" entities, which have been initialized by name
	 * @param creator function for creation
	 * @param afterCreate consumer for the collection of created entities
	 */
	public void createMissingMapped(Function<String, T> creator, Consumer<Collection<T>> afterCreate) {
		Set<String> namesToCreate = entitiesByName.entrySet().stream()
				.filter(entry -> mappedNames.contains(entry.getKey()))
				.filter(entry -> entry.getValue() == null).map(entry -> entry.getKey())
				.collect(Collectors.toSet());
		List<T> created = new ArrayList<>();
		for (String name : namesToCreate) {
			T entity = creator.apply(name);
			created.add(entity);
			entitiesByUuid.put(entity.getUuid(), entity);
			entitiesByName.put(entity.getName(), entity);
			createdUuids.add(entity.getUuid());
		}
		if (!created.isEmpty()) {
			afterCreate.accept(created);
		}
	}

	/**
	 * Check whether the given entity was created via {@link #createMissingMapped(Function, Consumer)}
	 * @param entity entity to check
	 * @return true if the entity was created
	 */
	public boolean wasCreated(T entity) {
		return createdUuids.contains(entity.getUuid());
	}

	/**
	 * Get all entities handled by this helper
	 * @return collection of handled entities (loaded or created)
	 */
	public Collection<T> getAllEntities() {
		return entitiesByUuid.values();
	}

	/**
	 * Get the mapped entities handled by this helper
	 * @return collection of mapped entities (loaded or created)
	 */
	public Collection<T> getMappedEntities() {
		return getAllEntities().stream()
				.filter(entity -> mappedUuids.contains(entity.getUuid()) || mappedNames.contains(entity.getName()))
				.collect(Collectors.toList());
	}

	/**
	 * Get the handled entities with given uuids
	 * @param uuids uuids of entities to return
	 * @return collection of handled entities with the given uuids
	 */
	public Collection<T> getEntities(Collection<String> uuids) {
		return uuids.stream().map(uuid -> entitiesByUuid.get(uuid)).filter(group -> group != null)
				.collect(Collectors.toList());
	}

	/**
	 * Get the entity with given uuid or name as {@link Optional}
	 * @param uuid entity uuid
	 * @param name entity name
	 * @return optional entity
	 */
	public Optional<T> getEntity(String uuid, String name) {
		if (!StringUtils.isEmpty(uuid) && entitiesByUuid.containsKey(uuid)) {
			return Optional.ofNullable(entitiesByUuid.get(uuid));
		} else if (!StringUtils.isEmpty(name) && entitiesByName.containsKey(name)) {
			return Optional.ofNullable(entitiesByName.get(name));
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Possible order for handling uuid or name of an entity
	 */
	public static enum Order {
		/**
		 * Uuid is handled before name
		 */
		UUID_FIRST,

		/**
		 * Name is handled before uuid
		 */
		NAME_FIRST;
	}
}
