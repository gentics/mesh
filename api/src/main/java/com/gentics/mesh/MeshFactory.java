package com.gentics.mesh;

import com.gentics.mesh.etc.config.MeshOptions;

/**
 * Mesh factory which provides new instances of mesh.
 */
public interface MeshFactory {

	/**
	 * Check whether the factory has already provided a mesh instance.
	 * 
	 * @return
	 */
	boolean isInitalized();

	/**
	 * Return a new or the current instance of mesh.
	 * 
	 * @return Mesh instance
	 */
	Mesh mesh();

	/**
	 * Return a new instance of mesh.
	 * 
	 * @param options
	 *            Mesh options
	 * @return Mesh instance
	 */
	Mesh mesh(MeshOptions options);

}
