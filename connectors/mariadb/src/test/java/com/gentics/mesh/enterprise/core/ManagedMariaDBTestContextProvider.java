package com.gentics.mesh.enterprise.core;

import java.util.List;

import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.enterprise.database.HibernateTx;
import com.gentics.mesh.enterprise.database.connector.MariaDBConnector;
import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;

import jakarta.persistence.EntityManager;

/**
 * Test context provider for using a managed MariaDB
 */
@ManagedBy(name = "mariadb", connector = MariaDBConnector.class)
public class ManagedMariaDBTestContextProvider extends ManagedDatabaseTestContextProvider {
	@Override
	protected String getConnectionUrl() {
		return "jdbc:mariadb://" + dbSettings.getHost() + ":" + dbSettings.getPort() + "/";
	}

	@Override
	public boolean fastStorageCleanup(Database db) throws Exception {
		return db.tx(tx -> {
			EntityManager em = tx.<HibernateTx>unwrap().entityManager();
			em.createNativeQuery("SET FOREIGN_KEY_CHECKS=0").executeUpdate();
			String findAllMeshTablesQuery = "SELECT table_name FROM information_schema.tables WHERE table_name LIKE :prefix AND table_schema = :schema";
			List<?> tableNames = em.createNativeQuery(findAllMeshTablesQuery)
					.setParameter("prefix", MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "%")
					.setParameter("schema", dbSettings.database).getResultList();

			tableNames.forEach(tableName -> {
				String statement = "truncate table " + tableName;
				em.createNativeQuery(statement).executeUpdate();
			});
			em.createNativeQuery("SET FOREIGN_KEY_CHECKS=1").executeUpdate();
			return true;
		});
	}
}
