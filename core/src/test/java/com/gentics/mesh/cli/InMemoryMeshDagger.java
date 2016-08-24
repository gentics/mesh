package com.gentics.mesh.cli;

import javax.inject.Singleton;

import com.gentics.mesh.core.verticle.user.UserVerticle;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.test.SpringTestConfiguration;

import dagger.Component;

@Singleton
@Component(modules = { SpringTestConfiguration.class })
public interface InMemoryMeshDagger {

	BootstrapInitializer boot();

	Database database();
	
	UserVerticle userVerticle();
	
	SpringTestConfiguration config();
}
