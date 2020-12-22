package com.gentics.mesh.cli;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;

/**
 * Minimalistic mesh runner
 */
public class MeshRunner {

	/**
	 * Start mesh.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		MeshOptions options = OptionsLoader.createOrloadOptions(OrientDBMeshOptions.class, args);
		Mesh mesh = Mesh.create(options);
		mesh.run();
	}

}
