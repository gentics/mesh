
package com.gentics.mesh;

import java.util.Set;

import com.gentics.mesh.etc.MeshCustomLoader;
import com.gentics.mesh.etc.config.MeshOptions;

import io.reactivex.Completable;
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
	static Mesh create(MeshOptions options) {
		options.validate();
		return factory.create(options);
	}

	/**
	 * Returns the initialized instance of mesh that was created using the given options.
	 * 
	 * @return Fluent API
	 */
	static Mesh create() {
		return factory.create();
	}

	/**
	 * Stop the the Mesh instance and release any resources held by it.
	 * 
	 * The instance cannot be used after it has been closed.
	 * Please note that this call may not terminate the JVM.
	 * 
	 * @throws Exception
	 */
	void shutdown() throws Exception;

	/**
	 * Shutdown the instance and terminate the JVM.
	 * 
	 * @param code Exit code to return
	 */
	void shutdownAndTerminate(int code);

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
	 * Start Gentics Mesh. This will effectively block until {@link #shutdown()} is called from another thread. This method will initialise the dagger context and
	 * deploy mandatory verticles and extensions.
	 * 
	 * @throws Exception
	 * @return Fluent API
	 */
	Mesh run() throws Exception;

	/**
	 * Start Gentics Mesh and complete the completable once the server is ready.
	 * 
	 * @return
	 */
	default Completable rxRun() {
		return Completable.fromAction(() -> {
			run(false);
		});
	}

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
	 * Wait until shutdown has been invoked.
	 * 
	 * @throws InterruptedException
	 */
	void dontExit() throws InterruptedException;

	/**
	 * Returns the internal Mesh API.
	 * 
	 * @param <T>
	 * @return
	 */
	<T> T internal();

	/**
	 * Set the dagger mesh component reference which allows access to the internal API.
	 * 
	 * @param <T>
	 * @param meshInternal
	 */
	<T> void setMeshInternal(T meshInternal);

	/**
	 * Deploy the plugin with the given class.
	 * 
	 * @param clazz
	 * @param id
	 *            The id of the plugin (e.g. hello-world)
	 * @return
	 */
	Completable deployPlugin(Class<?> clazz, String id);

	/**
	 * Return a set of deployed plugin ids.
	 * 
	 * @return
	 */
	Set<String> pluginIds();

}
