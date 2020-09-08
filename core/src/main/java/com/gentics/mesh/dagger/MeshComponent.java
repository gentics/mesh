package com.gentics.mesh.dagger;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Provider;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.endpoint.role.RoleCrudHandler;
import com.gentics.mesh.core.migration.BranchMigration;
import com.gentics.mesh.core.migration.MicronodeMigration;
import com.gentics.mesh.core.migration.NodeMigration;
import com.gentics.mesh.core.project.maintenance.ProjectVersionPurgeHandler;
import com.gentics.mesh.core.verticle.job.JobWorkerVerticle;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.router.EndpointRegistry;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.router.RouterStorageRegistry;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.TrackingSearchProvider;

/**
 * Central dagger mesh component which will expose dependencies.
 */
public interface MeshComponent extends BaseMeshComponent {

	BootstrapInitializer boot();

	Database database();

	EndpointRegistry endpointRegistry();

	Provider<RouterStorage> routerStorageProvider();

	default TrackingSearchProvider trackingSearchProvider() {
		return (TrackingSearchProvider) searchProvider();
	}

	JobWorkerVerticle jobWorkerVerticle();

	ServerSchemaStorage serverSchemaStorage();

	NodeMigration nodeMigrationHandler();

	BranchMigration branchMigrationHandler();

	MicronodeMigration micronodeMigrationHandler();

	ProjectVersionPurgeHandler projectVersionPurgeHandler();

	IndexHandlerRegistry indexHandlerRegistry();

	Provider<EventQueueBatch> batchProvider();

	RouterStorageRegistry routerStorageRegistry();

	Binaries binaries();

	RoleCrudHandler roleCrudHandler();

	List<ConsistencyCheck> consistencyChecks();

	interface Builder {
		Builder configuration(MeshOptions options);

		Builder mesh(Mesh mesh);

		Builder searchProviderType(@Nullable SearchProviderType type);

		MeshComponent build();
	}
}
