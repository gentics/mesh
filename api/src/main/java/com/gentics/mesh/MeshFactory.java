package com.gentics.mesh;

import com.gentics.mesh.etc.config.MeshOptions;

public interface MeshFactory {

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
