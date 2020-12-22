package com.gentics.mesh;

import com.gentics.mesh.etc.config.AbstractMeshOptions;

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
	 * @return Mesh instance
	 */
	Mesh create(AbstractMeshOptions options);
}
