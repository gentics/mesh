package com.gentics.mesh.demo;

import com.gentics.mesh.etc.config.OrientDBMeshOptions;

/**
 * The demo dump generator is used to create a mesh database dump which contains the demo data. This dump is packaged and later placed within the final mesh jar
 * in order to accelerate demo startup.
 */
public class DemoDumpGenerator extends AbstractDemoDumper<OrientDBMeshOptions> {

	static {
		System.setProperty("memory.directMemory.preallocate", "false");
	}

	public DemoDumpGenerator(String[] args) {
		super(args, OrientDBMeshOptions.class);
	}

	/**
	 * Run the demo dump generator which will write the dump to the filesystem.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		DemoDumpGenerator generator = new DemoDumpGenerator(args);
		try {
			generator.cleanup();
			generator.init();
			generator.dump();
			generator.shutdown();
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void setupOptions(OrientDBMeshOptions options) {
		// The database provider will switch to in memory mode when no directory has been specified.
		options.getStorageOptions().setDirectory("target/dump/" + options.getStorageOptions().getDirectory());
	}
}
