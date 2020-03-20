package com.gentics.mesh.dagger;

import javax.inject.Provider;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.auth.handler.MeshJWTAuthHandler;
import com.gentics.mesh.auth.provider.MeshJWTAuthProvider;
import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.cache.ProjectBranchNameCache;
import com.gentics.mesh.cache.ProjectNameCache;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.generic.UserProperties;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.migration.node.NodeMigrationHandler;
import com.gentics.mesh.core.project.maintenance.ProjectVersionPurgeHandler;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.manager.MeshPluginManager;
import com.gentics.mesh.router.EndpointRegistry;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.router.RouterStorageRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.TrackingSearchProvider;
import com.gentics.mesh.storage.BinaryStorage;

import io.vertx.core.Vertx;

public interface MeshComponentBase {


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

//	JobWorkerVerticle jobWorkerVerticle();
//
//	ServerSchemaStorage serverSchemaStorage();
//
//	NodeIndexHandler nodeContainerIndexHandler();
//
	NodeMigrationHandler nodeMigrationHandler();
//
//	BranchMigrationHandler branchMigrationHandler();
//
//	MicronodeMigrationHandler micronodeMigrationHandler();
//
	ProjectVersionPurgeHandler projectVersionPurgeHandler();
//
//	MeshLocalClientImpl meshLocalClientImpl();

	WebRootLinkReplacer webRootLinkReplacer();

//	IndexHandlerRegistry indexHandlerRegistry();
//
//	LocalBinaryStorage localBinaryStorage();
//
//	ProjectIndexHandler projectIndexHandler();
//
//	UserIndexHandler userIndexHandler();
//
//	RoleIndexHandler roleIndexHandler();
//
//	GroupIndexHandler groupIndexHandler();
//
//	SchemaContainerIndexHandler schemaContainerIndexHandler();
//
//	MicroschemaContainerIndexHandler microschemaContainerIndexHandler();
//
//	TagIndexHandler tagIndexHandler();
//
//	TagFamilyIndexHandler tagFamilyIndexHandler();
//
//	BinaryUploadHandler nodeFieldAPIHandler();

	ImageManipulator imageManipulator();

//	SchemaComparator schemaComparator();

//	RestAPIVerticle restApiVerticle();

	MeshJWTAuthProvider authProvider();

	MetricsService metrics();

	ProjectBranchNameCache branchCache();

	ProjectNameCache projectNameCache();

	PermissionCache permissionCache();

	Vertx vertx();

	Provider<EventQueueBatch> batchProvider();

	Provider<BulkActionContext> bulkProvider();

	MeshOptions options();

	PluginEnvironment pluginEnv();

	MeshPluginManager pluginManager();

	RouterStorageRegistry routerStorageRegistry();

	Binaries binaries();

	UserProperties userProperties();

	PermissionProperties permissionProperties();

}
