package com.gentics.mesh.core;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;
import com.gentics.mesh.test.MeshOptionsProvider;
import com.gentics.mesh.test.MeshTestContextProvider;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.docker.InMemoryDatabase;
import com.gentics.mesh.util.UUIDUtil;

import jakarta.persistence.EntityManager;

public class HSQLMemoryTestContextProvider extends HibernateTestContextProvider implements MeshTestContextProvider, HSQLMemoryOptionsProvider {
	
	private static InMemoryDatabase server;

	static {
		System.setProperty(MeshOptionsProvider.ENV_OPTIONS_PROVIDER_CLASS, HSQLMemoryTestContextProvider.class.getCanonicalName());
	}

	@Override
	public void initPhysicalStorage(MeshTestSetting settings) throws IOException {
		File directory = null;
		if (!settings.inMemoryDB() || settings.clusterMode()) {
			directory = new File("target/graphdb_" + UUIDUtil.randomUUID());
			directory.deleteOnExit();
			directory.mkdirs();
		}
		server = new InMemoryDatabase();
		if (settings.inMemoryDB() || settings.startStorageServer()) {
			int port = HibernateTestUtils.pickLocalPort(false);
			meshOptions.getStorageOptions().setDatabaseAddress("localhost:" + port);
			server.start(meshOptions.getStorageOptions().getDatabaseName(), port, Optional.ofNullable(directory), false);
		} else {
			meshOptions.getStorageOptions().setDatabaseAddress(StringUtils.EMPTY);
			server = null;
		}
	}

	@Override
	public void teardownStorage() {
		if (server != null) {
			server.stop();
			server = null;
		}
	}

	@Override
	public boolean fastStorageCleanup(Database db) throws Exception {
		return db.tx(tx -> {
			EntityManager em = tx.<HibernateTx>unwrap().entityManager();
			log.info("Clearing the test HSQL database...");
			em.createNativeQuery("SET DATABASE REFERENTIAL INTEGRITY FALSE").executeUpdate();
			String findAllMeshTablesQuery = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE' AND UPPER(TABLE_NAME) LIKE UPPER(:prefix)";
			List<?> tableNames = em.createNativeQuery(findAllMeshTablesQuery).setParameter("prefix", "%" + MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "%").getResultList();

			tableNames.forEach(tableName -> {
				// we cannot use truncate since removing the referential integrity doesn't work for it: https://stackoverflow.com/questions/63556383/truncate-table-with-referential-constraint?rq=1
				String statement = "delete from " + tableName;
				em.createNativeQuery(statement).executeUpdate();
			});
			em.createNativeQuery("SET DATABASE REFERENTIAL INTEGRITY TRUE").executeUpdate();
			return true;
		});
	}
}
