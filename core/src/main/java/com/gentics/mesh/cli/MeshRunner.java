package com.gentics.mesh.cli;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
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
		MeshOptions options = OptionsLoader.createOrloadOptions(args);
		Mesh mesh = Mesh.create(options);
		mesh.run();
	}

}
