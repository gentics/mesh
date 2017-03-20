package com.gentics.mesh;

import java.util.Properties;

import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.ServiceHelper;
import io.vertx.core.Vertx;

/**
 * The main mesh interface which exposes various methods that can be used to initialize mesh and startup a new instance.
 */
public interface Mesh {

	public static final String STARTUP_EVENT_ADDRESS = "mesh-startup-complete";

	static final MeshFactory factory = ServiceHelper.loadFactory(MeshFactory.class);

	/**
	 * Returns the initialized instance.
	 * 
	 * @return Fluent API
	 */
	static Mesh mesh() {
		return factory.mesh();
	}

	/**
	 * Returns the initialized instance of mesh that was created using the given options.
	 * 
	 * @param options
	 * @return Fluent API
	 */
	static Mesh mesh(MeshOptions options) {
		return factory.mesh(options);
	}

	/**
	 * Return the mesh build information.
	 * 
	 * @return Mesh version and build timestamp.
	 */
	static BuildInfo getBuildInfo() {
		try {
			Properties buildProperties = new Properties();
			buildProperties.load(Mesh.class.getResourceAsStream("/mesh.build.properties"));
			return new BuildInfo(buildProperties);
		} catch (Exception e) {
			return new BuildInfo("unknown", "unknown");
		}
		// Package pack = MeshImpl.class.getPackage();
		// return pack.getImplementationVersion();
	}

	/**
	 * Return the mesh version (without build timestamp)
	 *
	 * @return Mesh version
	 */
	static String getPlainVersion() {
		try {
			Properties buildProperties = new Properties();
			buildProperties.load(Mesh.class.getResourceAsStream("/mesh.build.properties"));
			return buildProperties.get("mesh.version")
					.toString();
		} catch (Exception e) {
			return "Unknown";
		}
	}

	/**
	 * Stop the the Mesh instance and release any resources held by it.
	 * 
	 * The instance cannot be used after it has been closed.
	 * 
	 * @throws Exception
	 */
	void shutdown() throws Exception;

	/**
	 * Set a custom verticle loader that will be invoked once all major components have been initialized.
	 * 
	 * @param verticleLoader
	 */
	void setCustomLoader(MeshCustomLoader<Vertx> verticleLoader);

	/**
	 * Return the mesh options.
	 * 
	 * @return Mesh options
	 */
	MeshOptions getOptions();

	/**
	 * Start mesh. This will effectively block until {@link #shutdown()} is called from another thread.
	 * 
	 * @throws Exception
	 */
	void run() throws Exception;

	/**
	 * Return the vertx instance for mesh.
	 * 
	 * @return Vertx instance
	 */
	Vertx getVertx();

	/**
	 * Returns the used vertx instance for mesh.
	 * 
	 * @return Vertx instance
	 */
	public static Vertx vertx() {
		return factory.mesh()
				.getVertx();
	}

	public static void main(String[] args) throws Exception {
		Mesh.mesh()
				.run();
	}

}
