package com.gentics.mesh.demo;

import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.test.MeshOptionsProvider;

public class OrientDBMeshDemoContextProvider implements MeshDemoContextProvider {
	
	static {
		System.setProperty(MeshDemoContextProvider.ENV_DEMO_CONTEXT_PROVIDER_CLASS, OrientDBMeshDemoContextProvider.class.getCanonicalName());
		System.setProperty(MeshOptionsProvider.ENV_OPTIONS_PROVIDER_CLASS, OrientDBMeshDemoContextProvider.class.getCanonicalName());
	}

	private final DemoDumper demoDumper;
	private final OrientDBMeshOptions meshOptions;

	public OrientDBMeshDemoContextProvider() {
		this.meshOptions = OptionsLoader.generateDefaultConfig(OrientDBMeshOptions.class, null);
		this.demoDumper = new DemoDumpGenerator(new String[0]);
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