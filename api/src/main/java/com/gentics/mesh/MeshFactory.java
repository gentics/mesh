package com.gentics.mesh;

import com.gentics.mesh.etc.config.MeshOptions;

/**
 * Mesh factory which provides new instances of mesh.
 */
public interface MeshFactory {

	/**
	 * Return a new or the current instance of mesh.
	 * 
	 * @return Mesh instance
	 * @throws Exception 
	 */
	Mesh mesh();

	/**
	 * Return a new instance of mesh.
	 * 
	 * @param options
	 *            Mesh options
	 * @param args
	 *            Additional command line args
	 * @return Mesh instance
	 * @throws Exception 
	 */
	Mesh mesh(MeshOptions options, String... args);

}
