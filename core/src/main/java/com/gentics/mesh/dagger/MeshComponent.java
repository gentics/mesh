package com.gentics.mesh.dagger;

import java.util.List;

import javax.annotation.Nullable;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Central dagger mesh component which will expose dependencies.
 */
public interface MeshComponent extends BaseMeshComponent {

	BootstrapInitializer boot();

	Database database();

	List<ConsistencyCheck> consistencyChecks();

	interface Builder {
		Builder configuration(MeshOptions options);

		Builder mesh(Mesh mesh);

		Builder searchProviderType(@Nullable SearchProviderType type);

		MeshComponent build();
	}
}
