package com.gentics.mesh.demo;

import com.gentics.mesh.etc.config.OrientDBMeshOptions;

/**
 * Main runner that is used to deploy a preconfigured set of verticles.
 */
public class DemoRunner extends AbstractDemoRunner<OrientDBMeshOptions> {

	static {
		System.setProperty("memory.directMemory.preallocate", "false");
		System.setProperty("storage.trackChangedRecordsInWAL", "true");
	}

	public DemoRunner(String[] args) {
		super(args, OrientDBMeshOptions.class);
	}

	/**
	 * Start the process.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		new DemoRunner(args).run();
	}
}
