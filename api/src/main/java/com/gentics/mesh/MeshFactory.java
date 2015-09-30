package com.gentics.mesh;

import com.gentics.mesh.etc.config.MeshOptions;

public interface MeshFactory {

	/**
	 * Return a new or the current instance of mesh.
	 * 
	 * @return
	 */
	Mesh mesh();

	/**
	 * Return a new instance of mesh.
	 * 
	 * @param options
	 * @return
	 */
	Mesh mesh(MeshOptions options);

}
