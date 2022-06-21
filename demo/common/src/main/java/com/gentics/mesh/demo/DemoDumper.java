package com.gentics.mesh.demo;

import java.io.IOException;

import com.gentics.mesh.dagger.MeshComponent;

/**
 * Dump the demo data into the external storage.
 * 
 * @author plyhun
 *
 */
public interface DemoDumper {

	/**
	 * Run the dumper.
	 * 
	 * @throws Exception
	 */
	void dump() throws Exception;

	/**
	 * Get actual Mesh instance out of demo context
	 * 
	 * @return
	 */
	MeshComponent getMeshInternal();

	/**
	 * Initialize mesh and prepare the demo dumper.
	 * 
	 * @throws Exception
	 */
	void init() throws Exception;

	/**
	 * Cleanup the dump directories and remove the existing mesh configuration.
	 * 
	 * @throws IOException
	 */
	void cleanup() throws IOException;
}