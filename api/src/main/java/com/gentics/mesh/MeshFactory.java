package com.gentics.mesh;

import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.Vertx;

/**
 * Mesh factory which provides new instances of mesh.
 */
public interface MeshFactory {

	/**
	 * Return a new instance of mesh.
	 * 
	 * @return Mesh instance
	 */
	Mesh create();

	/**
	 * Return a new instance of mesh.
	 * 
	 * @param options
	 *            Mesh options
	 * @param vertx
	 *            Vert.x instance to be used
	 * @return Mesh instance
	 */
	Mesh create(MeshOptions options, Vertx vertx);

	/**
	 * Return a new instance of mesh.
	 * 
	 * @param options
	 *            Mesh options
	 * @return Mesh instance
	 */
	default Mesh create(MeshOptions options) {
		return create(options, null);
	}

}
