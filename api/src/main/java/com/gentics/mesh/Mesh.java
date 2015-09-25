package com.gentics.mesh;

import java.util.Properties;

import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.ServiceHelper;
import io.vertx.core.Vertx;

public interface Mesh {

	/**
	 * Returns the initialized instance.
	 * 
	 * @return
	 * @throws MeshConfigurationException
	 */
	static Mesh mesh() {
		return factory.mesh();
	}

	static Mesh mesh(MeshOptions options) {
		return factory.mesh(options);
	}

	static String getVersion() {
		try {
			Properties buildProperties = new Properties();
			buildProperties.load(Mesh.class.getResourceAsStream("/mesh.build.properties"));
			return buildProperties.get("mesh.version") + " " + buildProperties.get("mesh.build.timestamp");
		} catch (Exception e) {
			return "Unknown";
		}
		//Package pack = MeshImpl.class.getPackage();
		//return pack.getImplementationVersion();
	}

	/**
	 * Stop the the Mesh instance and release any resources held by it.
	 * <p>
	 * The instance cannot be used after it has been closed.
	 */
	void shutdown();

	void setCustomLoader(MeshCustomLoader<Vertx> verticleLoader);

	MeshOptions getOptions();

	void run() throws Exception;

	/**
	 * Return the vertx instance for mesh.
	 * 
	 * @return
	 */
	Vertx getVertx();

	/**
	 * Returns the used vertx instance for mesh.
	 * 
	 * @return
	 */
	public static Vertx vertx() {
		return factory.mesh().getVertx();
	}

	static final MeshFactory factory = ServiceHelper.loadFactory(MeshFactory.class);

}
