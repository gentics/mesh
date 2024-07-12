package com.gentics.mesh.plugin.test.common;

import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.enterprise.database.connector.HSQLDBConnector;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.test.MeshOptionsProvider;
import com.gentics.mesh.test.MeshTestContextProvider;
import com.gentics.mesh.test.local.MeshLocalServer;
import com.gentics.mesh.enterprise.core.HSQLMemoryTestContextProvider;

/**
 * OrientDB specific extension to {@link MeshLocalServer}
 */
public class HibernateMeshLocalServer extends MeshLocalServer {

	static {
		System.setProperty(MeshOptionsProvider.ENV_OPTIONS_PROVIDER_CLASS, HSQLMemoryTestContextProvider.class.getCanonicalName());
		// this is necessary, so that hibernate will log using slf4j
		System.setProperty("org.jboss.logging.provider", "slf4j");
	}

	/**
	 * Create an instance
	 */
	public HibernateMeshLocalServer() {
		super(MeshTestContextProvider.getProvider());
		HibernateMeshOptions options = (HibernateMeshOptions) getMeshOptions();
		DatabaseConnector connector = new HSQLDBConnector(options);
		options.getStorageOptions().setDriverClass(connector.getJdbcDriverClass().getCanonicalName());
		options.getStorageOptions().setDialectClass(connector.getHibernateDialectClass().getCanonicalName());
	}
}
