package com.gentics.mesh.cli;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.etc.config.MeshOptions;

public class MeshRunner {

	public static void main(String[] args) throws Exception {
		MeshOptions options = OptionsLoader.createOrloadOptions();
		Mesh mesh = Mesh.mesh(options, args);
		mesh.run();
	}

}
