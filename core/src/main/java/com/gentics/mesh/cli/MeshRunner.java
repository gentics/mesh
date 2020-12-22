package com.gentics.mesh.cli;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.etc.config.MeshOptions;

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
		AbstractMeshOptions options = OptionsLoader.createOrloadOptions(MeshOptions.class, args);
		Mesh mesh = Mesh.create(options);
		mesh.run();
	}

}
