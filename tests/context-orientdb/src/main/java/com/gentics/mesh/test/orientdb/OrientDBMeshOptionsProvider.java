package com.gentics.mesh.test.orientdb;


import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.demo.DemoDumpGenerator;
import com.gentics.mesh.demo.DemoDumper;
import com.gentics.mesh.demo.MeshDemoContextProvider;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;

/**
 * 
 * @author plyhun
 *
 */
public class OrientDBMeshOptionsProvider implements MeshDemoContextProvider {

	private final OrientDBMeshOptions meshOptions;
	private final DemoDumper demoDumper;
	
	public OrientDBMeshOptionsProvider() {
		meshOptions = OptionsLoader.generateDefaultConfig(OrientDBMeshOptions.class, null);
		demoDumper = new DemoDumpGenerator(new String[0]);
	}
	
	@Override
	public MeshOptions getOptions() {
		return meshOptions;
	}

	@Override
	public DemoDumper getDumper() {
		return demoDumper;
	}
}
