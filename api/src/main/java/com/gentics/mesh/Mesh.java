
package com.gentics.mesh;

import java.util.Map;

import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.config.MeshOptions;

import io.reactivex.Single;
import io.vertx.core.ServiceHelper;
import io.vertx.core.Vertx;

/**
 * The main mesh interface which exposes various methods that can be used to initialize mesh and startup a new instance.
 */
public interface Mesh {

	static MeshFactory factory = ServiceHelper.loadFactory(MeshFactory.class);

	/**
	 * Returns the initialized instance.
	 * 
	 * @param options
	 * 
	 * @return Fluent API
	 */
	static Mesh mesh(MeshOptions options) {
		return factory.mesh(options);
	}

	/**
	 * Returns the initialized instance of mesh that was created using the given options.
	 * 
	 * @return Fluent API
	 */
	static Mesh mesh() {
		return factory.mesh();
	}

	/**
	 * Check whether Gentics Mesh has already been initialized.
	 * 
	 * @return
	 */
	static boolean isInitalized() {
		return factory.isInitalized();
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
	 * Start mesh. This will effectively block until {@link #shutdown()} is called from another thread. This method will initialise the dagger context and
	 * deploy mandatory verticles and extensions.
	 * 
	 * @throws Exception
	 * @return Fluent API
	 */
	Mesh run() throws Exception;

	/**
	 * Start mesh
	 * 
	 * @param block
	 *            Whether or not to block the further execution. This is useful if you want to run mesh from a main method
	 * @throws Exception
	 * @return Fluent API
	 */
	Mesh run(boolean block) throws Exception;

	/**
	 * Return the vertx instance for mesh.
	 * 
	 * @return Vertx instance
	 */
	Vertx getVertx();

	/**
	 * Return the RX variant of the vertx instance for mesh.
	 * 
	 * @return
	 */
	io.vertx.reactivex.core.Vertx getRxVertx();

	/**
	 * Returns the used vertx instance for mesh.
	 * 
	 * @return Vertx instance
	 */
	public static Vertx vertx() {
		return factory.mesh().getVertx();
	}

	/**
	 * Return the used vertx (rx variant) instance for mesh.
	 * 
	 * @return Rx Vertx instance
	 */
	public static io.vertx.reactivex.core.Vertx rxVertx() {
		return factory.mesh().getRxVertx();
	}

	public static void main(String[] args) throws Exception {
		Mesh.mesh().run();
	}

	/**
	 * Set the current server status.
	 * 
	 * @param status
	 * @return
	 */
	Mesh setStatus(MeshStatus status);

	/**
	 * Return the current server status.
	 * 
	 * @return
	 */
	MeshStatus getStatus();

	static String getPlainVersion() {
		return MeshVersion.getPlainVersion();
	}

	/**
	 * Check whether Vert.x ready to be used.
	 * 
	 * @return
	 */
	static boolean isVertxReady() {
		return vertx() != null;
	}

	/**
	 * Wait until shutdown has been invoked.
	 * 
	 * @throws InterruptedException
	 */
	void dontExit() throws InterruptedException;

	/**
	 * Deploy the plugin with the given class.
	 * 
	 * @param clazz
	 * @param id
	 *            The id of the plugin (e.g. hello-world)
	 * @return Single which will return the deployment uuid
	 */
	Single<String> deployPlugin(Class<?> clazz, String id);

	/**
	 * Return a map of deployed plugins with name and id.
	 * 
	 * @return
	 */
	Map<String, String> pluginUuids();

}
