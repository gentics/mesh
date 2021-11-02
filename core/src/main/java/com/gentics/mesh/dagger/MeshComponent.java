package com.gentics.mesh.dagger;

import java.util.List;

import javax.annotation.Nullable;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshFactory;
import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.dagger.module.SearchProviderModule;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.MeshEventSender;

import com.gentics.mesh.monitor.liveness.LivenessManager;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.BucketManager;

/**
 * Central dagger mesh component which will expose dependencies.
 */
public interface MeshComponent extends BaseMeshComponent {

	@Getter
	BootstrapInitializer boot();

	@Getter
	Database database();

	@Getter
	List<ConsistencyCheck> consistencyChecks();

	@Getter
	BucketManager bucketManager();

	@Getter
	MeshEventSender eventSender();

	@Getter
	LivenessManager livenessManager();

	/**
	 * Builder for the main dagger component. It allows injection of options and the mesh instance which will be created by the {@link MeshFactory} outside of
	 * dagger.
	 */
	interface Builder {
		/**
		 * Inject configuration options.
		 * 
		 * @param options
		 * @return
		 */
		Builder configuration(MeshOptions options);

		/**
		 * Inject mesh instance.
		 * 
		 * @param mesh
		 * @return
		 */
		Builder mesh(Mesh mesh);

		/**
		 * Inject the search provider type. The {@link SearchProviderModule} will utilize this info to select the correct {@link SearchProvider} for DI.
		 * 
		 * @param type
		 * @return
		 */
		Builder searchProviderType(@Nullable SearchProviderType type);

		/**
		 * Build the component.
		 * 
		 * @return
		 */
		MeshComponent build();
	}
}
