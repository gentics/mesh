package com.gentics.mesh.core;

import java.util.List;

import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;
import com.gentics.mesh.test.MeshInstanceProvider;

import jakarta.persistence.EntityManager;

/**
 * Common MariaDB code.
 */
public interface MariaDBTestContextProviderBase extends MeshInstanceProvider<HibernateMeshOptions> {

	@Override
	default boolean fastStorageCleanup(List<Database> dbs) throws Exception {
		for (Database db : dbs) {
			db.tx(tx -> {
				EntityManager em = tx.<HibernateTx>unwrap().entityManager();
				em.createNativeQuery("SET FOREIGN_KEY_CHECKS=0").executeUpdate();
				String findAllMeshTablesQuery = "SELECT table_name FROM information_schema.tables WHERE table_name LIKE :prefix AND table_schema = :schema";
				List<?> tableNames = em.createNativeQuery(findAllMeshTablesQuery)
						.setParameter("prefix", MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "%")
						.setParameter("schema", getOptions().getStorageOptions().getDatabaseName()).getResultList();
	
				tableNames.forEach(tableName -> {
					String statement = "truncate table " + tableName;
					em.createNativeQuery(statement).executeUpdate();
				});
				em.createNativeQuery("SET FOREIGN_KEY_CHECKS=1").executeUpdate();
				return true;
			});
		}
		return true;
	}
}
