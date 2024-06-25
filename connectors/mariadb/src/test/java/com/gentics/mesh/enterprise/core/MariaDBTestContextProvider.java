package com.gentics.mesh.enterprise.core;

import java.util.List;

import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.enterprise.database.HibernateTx;
import com.gentics.mesh.enterprise.etc.config.EnterpriseMeshOptions;
import com.gentics.mesh.enterprise.test.docker.DatabaseContainer;
import com.gentics.mesh.enterprise.test.docker.MariaDBDatabaseContainer;
import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;
import com.gentics.mesh.test.MeshOptionsProvider;
import com.gentics.mesh.test.MeshTestContextProvider;

import jakarta.persistence.EntityManager;

public class MariaDBTestContextProvider extends PreparingDatabaseTestContextProvider<MariaDBDatabaseContainer> implements MeshTestContextProvider {

	static {
		System.setProperty(MeshOptionsProvider.ENV_OPTIONS_PROVIDER_CLASS, MariaDBTestContextProvider.class.getCanonicalName());
	}

	public MariaDBTestContextProvider() {
		super(new MariaDBDatabaseContainer());
	}

	@Override
	public void fillMeshOptions(EnterpriseMeshOptions options) {
		super.fillMeshOptions(options);
		options.getStorageOptions().setConnectionUrl("jdbc:mariadb://localhost:" + getDatabaseContainer().getMappedPort() + "/");
		options.getStorageOptions().setDatabaseName(current);
		options.getStorageOptions().setConnectionUsername(DatabaseContainer.DEFAULT_USERNAME);
		options.getStorageOptions().setConnectionPassword(DatabaseContainer.DEFAULT_PASSWORD);
	}

	@Override
	public boolean fastStorageCleanup(Database db) throws Exception {
		return db.tx(tx -> {
			EntityManager em = tx.<HibernateTx>unwrap().entityManager();
			em.createNativeQuery("SET FOREIGN_KEY_CHECKS=0").executeUpdate();
			String findAllMeshTablesQuery = "SELECT table_name FROM information_schema.tables WHERE table_name LIKE :prefix AND table_schema = :schema";
			List<?> tableNames = em.createNativeQuery(findAllMeshTablesQuery)
					.setParameter("prefix", MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "%")
					.setParameter("schema", current).getResultList();

			tableNames.forEach(tableName -> {
				String statement = "truncate table " + tableName;
				em.createNativeQuery(statement).executeUpdate();
			});
			em.createNativeQuery("SET FOREIGN_KEY_CHECKS=1").executeUpdate();
			return true;
		});
	}
}
