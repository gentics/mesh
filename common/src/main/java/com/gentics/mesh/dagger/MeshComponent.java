package com.gentics.mesh.dagger;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshFactory;
import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.monitor.liveness.EventBusLivenessManager;
import com.gentics.mesh.monitor.liveness.LivenessManager;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.BucketManager;
import com.gentics.mesh.util.SearchWaitUtil;

/**
 * Central dagger mesh component which will expose dependencies.
 */
public interface MeshComponent extends BaseMeshComponent {

	@Getter
	BootstrapInitializer boot();

	@Getter
	List<ConsistencyCheck> consistencyChecks();

	@Getter
	BucketManager bucketManager();

	@Getter
	LivenessManager livenessManager();

	@Getter
	EventBusLivenessManager eventbusLivenessManager();

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
		 * Inject the own instance of {@link SearchWaitUtil}.
		 * 
		 * @param type
		 * @return
		 */
		Builder searchWaitUtilSupplier(@Nullable Supplier<SearchWaitUtil> swUtil);

		/**
		 * Build the component.
		 * 
		 * @return
		 */
		MeshComponent build();
	}
}
