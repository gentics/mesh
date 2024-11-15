package com.gentics.mesh.contentoperation;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.cfg.AvailableSettings;

import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.db.TxAction;
import com.gentics.mesh.database.HibernateDatabase;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;
import com.hazelcast.map.MapLoader;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;

/**
 * Loader used by hazelcast when keys are not found in the cache.
 */
public class HazelcastContentLoader implements MapLoader<ContentKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> {

	private HibernateDatabase db;
	private ContentNoCacheStorage storage;

	public HazelcastContentLoader(HibernateDatabase db, ContentNoCacheStorage storage) {
		this.db = db;
		this.storage = storage;
	}

	@Override
	public HibUnmanagedFieldContainer<?, ?, ?, ?, ?> load(ContentKey key) {
		return doBypassingCache(() -> storage.findOne(key));
	}

	@Override
	public Map<ContentKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> loadAll(Collection<ContentKey> keys) {
		return doBypassingCache(() -> storage.findMany(keys));
	}

	@Override
	public Iterable<ContentKey> loadAllKeys() {
		return doBypassingCache(() -> storage.findAllKeys());
	}

	/**
	 * Since the methods will be called by hazelcast, we must avoid to use the cache, since hazelcast calling itself
	 * can cause deadlocks.
	 * @param action
	 * @param <T>
	 * @return
	 */
	private <T> T doBypassingCache(Supplier<T> action) {
		TxAction<T> bypass = tx -> {
			byPassCache(tx.<HibernateTx>unwrap().entityManager());
			return action.get();
		};
		return Tx.maybeGet().map(bypass).orElseGet(() -> db.tx(bypass));
	}

	/**
	 * Make JPA bypassing the cache.
	 * 
	 * @param entityManager
	 */
	private void byPassCache(EntityManager entityManager) {
		entityManager.setProperty(AvailableSettings.JAKARTA_SHARED_CACHE_STORE_MODE, CacheStoreMode.BYPASS);
		entityManager.setProperty(AvailableSettings.JAKARTA_SHARED_CACHE_RETRIEVE_MODE, CacheRetrieveMode.BYPASS);
	}
}
