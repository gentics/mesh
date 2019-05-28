package com.gentics.mesh.dagger;

import javax.annotation.Nullable;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.auth.handler.MeshJWTAuthHandler;
import com.gentics.mesh.auth.provider.MeshJWTAuthProvider;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.endpoint.migration.branch.BranchMigrationHandler;
import com.gentics.mesh.core.endpoint.migration.micronode.MicronodeMigrationHandler;
import com.gentics.mesh.core.endpoint.migration.node.NodeMigrationHandler;
import com.gentics.mesh.core.endpoint.node.BinaryUploadHandler;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.project.maintenance.ProjectVersionPurgeHandler;
import com.gentics.mesh.core.verticle.job.JobWorkerVerticle;
import com.gentics.mesh.dagger.module.BindModule;
import com.gentics.mesh.dagger.module.MeshModule;
import com.gentics.mesh.dagger.module.SearchProviderModule;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.rest.MeshLocalClientImpl;
import com.gentics.mesh.rest.RestAPIVerticle;
import com.gentics.mesh.router.EndpointRegistry;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.TrackingSearchProvider;
import com.gentics.mesh.search.index.group.GroupIndexHandler;
import com.gentics.mesh.search.index.microschema.MicroschemaContainerIndexHandler;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
import com.gentics.mesh.search.index.project.ProjectIndexHandler;
import com.gentics.mesh.search.index.role.RoleIndexHandler;
import com.gentics.mesh.search.index.schema.SchemaContainerIndexHandler;
import com.gentics.mesh.search.index.tag.TagIndexHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.user.UserIndexHandler;
import com.gentics.mesh.storage.BinaryStorage;
import com.gentics.mesh.storage.LocalBinaryStorage;

import dagger.BindsInstance;
import dagger.Component;

/**
 * Central dagger mesh component which will expose dependencies.
 */
@Singleton
@Component(modules = { MeshModule.class, SearchProviderModule.class, BindModule.class })
public interface MeshComponent {

	BootstrapInitializer boot();

	Database database();

	EndpointRegistry endpointRegistry();

	SearchProvider searchProvider();

	BCryptPasswordEncoder passwordEncoder();

	Provider<RouterStorage> routerStorageProvider();

	BinaryStorage binaryStorage();

	default TrackingSearchProvider trackingSearchProvider() {
		return (TrackingSearchProvider) searchProvider();
	}

	MeshJWTAuthHandler authenticationHandler();

	JobWorkerVerticle jobWorkerVerticle();

	ServerSchemaStorage serverSchemaStorage();

	NodeIndexHandler nodeContainerIndexHandler();

	NodeMigrationHandler nodeMigrationHandler();

	BranchMigrationHandler branchMigrationHandler();

	MicronodeMigrationHandler micronodeMigrationHandler();

	ProjectVersionPurgeHandler projectVersionPurgeHandler();

	MeshLocalClientImpl meshLocalClientImpl();

	WebRootLinkReplacer webRootLinkReplacer();

	IndexHandlerRegistry indexHandlerRegistry();

	LocalBinaryStorage localBinaryStorage();

	ProjectIndexHandler projectIndexHandler();

	UserIndexHandler userIndexHandler();

	RoleIndexHandler roleIndexHandler();

	GroupIndexHandler groupIndexHandler();

	SchemaContainerIndexHandler schemaContainerIndexHandler();

	MicroschemaContainerIndexHandler microschemaContainerIndexHandler();

	TagIndexHandler tagIndexHandler();

	TagFamilyIndexHandler tagFamilyIndexHandler();

	BinaryUploadHandler nodeFieldAPIHandler();

	ImageManipulator imageManipulator();

	SchemaComparator schemaComparator();

	RestAPIVerticle restApiVerticle();

	MeshJWTAuthProvider authProvider();

	MetricsService metrics();

	@Component.Builder
	interface Builder {
		@BindsInstance
		Builder configuration(MeshOptions options);

		@BindsInstance
		Builder searchProviderType(@Nullable SearchProviderType type);

		MeshComponent build();
	}
}
