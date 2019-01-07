package com.gentics.mesh.madl;

import javax.inject.Singleton;

import com.gentics.madl.Madl;
import com.gentics.madl.server.ServerConfig;
import com.gentics.mesh.madl.MadlProvider;

@Singleton
public class OrientDBMadlProvider implements MadlProvider {

	private Madl madl;

	public OrientDBMadlProvider() {
	}

	public void init() {
		ServerConfig config = new ServerConfig().setClusterName("dummy").setNodeName("serverA").setClusterNameSuffix("latest");
		this.madl = Madl.madl("com.gentics.mesh.core.data", config);
	}

	public Madl getMadl() {
		return madl;
	}
}
