package com.gentics.mesh.demo;

import com.gentics.mesh.etc.config.MeshOptions;

/**
 * Set up the Mesh options for Demo mode.
 * 
 * @author plyhun
 *
 */
public interface MeshDemoOptionsProvider<T extends MeshOptions> {

	/**
	 * External impl-specific options setup.
	 * 
	 * @param options
	 * @throws Exception
	 */
	void setupOptions(T options) throws Exception;
}
