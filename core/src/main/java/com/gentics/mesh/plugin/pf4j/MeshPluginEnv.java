package com.gentics.mesh.plugin.pf4j;

import javax.inject.Inject;

import com.gentics.mesh.auth.provider.MeshJWTAuthProvider;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.plugin.env.PluginEnvironment;

public class MeshPluginEnv implements PluginEnvironment {

	@Inject
	public MeshPluginEnv() {
	}

	@Override
	public String adminToken() {
		MeshComponent mesh = MeshInternal.get();
		MeshJWTAuthProvider authProvider = mesh.authProvider();
		Database db = mesh.database();

		return db.tx(() -> {
			User admin = mesh.boot().userRoot().findByUsername("admin");
			// TODO: Use dedicated tokenCode - See https://github.com/gentics/mesh/issues/412
			return authProvider.generateAPIToken(admin, null, null);
		});
	}

}
