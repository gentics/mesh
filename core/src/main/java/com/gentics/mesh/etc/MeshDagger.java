package com.gentics.mesh.etc;

import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;

import dagger.Component;

@Singleton
@Component(modules = { MeshSpringConfiguration.class })
public interface MeshDagger {
	
	BootstrapInitializer boot();

}
