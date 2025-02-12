package com.gentics.mesh.contentoperation;

import java.io.Serializable;
import java.util.Properties;

import com.gentics.mesh.database.HibernateDatabase;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;
import com.hazelcast.map.MapLoader;
import com.hazelcast.map.MapStoreFactory;

/**
 * A factory used by Hazelcast to build the content loader. We have to do dependency injection ourselves since otherwise
 * hazelcast would build instances without the required dependencies.
 */
public class HazelcastContentLoaderFactory implements MapStoreFactory<ContentKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?>>, Serializable {

	private static final long serialVersionUID = -1144196361662510312L;

	private final HibernateDatabase hibernateDatabase;
	private final HibernateMeshOptions options;

	public HazelcastContentLoaderFactory(HibernateDatabase hibernateDatabase, HibernateMeshOptions options) {
		this.hibernateDatabase = hibernateDatabase;
		this.options = options;
	}

	@Override
	public MapLoader<ContentKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> newMapStore(String mapName, Properties properties) {
		return new HazelcastContentLoader(hibernateDatabase, new ContentNoCacheStorage(options, hibernateDatabase.getDatabaseConnector()));
	}
}
