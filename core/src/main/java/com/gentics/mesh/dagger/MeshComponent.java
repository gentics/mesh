package com.gentics.mesh.dagger;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Provider;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.gentics.mesh.Mesh;
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
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.endpoint.node.BinaryUploadHandler;
import com.gentics.mesh.core.endpoint.role.RoleCrudHandler;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.migration.impl.BranchMigrationImpl;
import com.gentics.mesh.core.migration.impl.MicronodeMigrationImpl;
import com.gentics.mesh.core.migration.impl.NodeMigrationImpl;
import com.gentics.mesh.core.project.maintenance.ProjectVersionPurgeHandler;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.core.verticle.job.JobWorkerVerticle;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.manager.MeshPluginManager;
import com.gentics.mesh.rest.MeshLocalClientImpl;
import com.gentics.mesh.rest.RestAPIVerticle;
import com.gentics.mesh.router.EndpointRegistry;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.router.RouterStorageRegistry;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.TrackingSearchProvider;
import com.gentics.mesh.search.index.group.GroupIndexHandler;
import com.gentics.mesh.search.index.microschema.MicroschemaContainerIndexHandler;
import com.gentics.mesh.search.index.node.NodeIndexHandlerImpl;
import com.gentics.mesh.search.index.project.ProjectIndexHandler;
import com.gentics.mesh.search.index.role.RoleIndexHandler;
import com.gentics.mesh.search.index.schema.SchemaContainerIndexHandler;
import com.gentics.mesh.search.index.tag.TagIndexHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.user.UserIndexHandler;
import com.gentics.mesh.storage.BinaryStorage;
import com.gentics.mesh.storage.LocalBinaryStorage;

import io.vertx.core.Vertx;

/**
 * Central dagger mesh component which will expose dependencies.
 */
public interface MeshComponent extends BaseMeshComponent {

	BootstrapInitializer boot();

	Database database();

	EndpointRegistry endpointRegistry();

	SearchProvider searchProvider();

	PasswordEncoder passwordEncoder();

	Provider<RouterStorage> routerStorageProvider();

	BinaryStorage binaryStorage();

	default TrackingSearchProvider trackingSearchProvider() {
		return (TrackingSearchProvider) searchProvider();
	}

	MeshJWTAuthHandler authenticationHandler();

	JobWorkerVerticle jobWorkerVerticle();

	ServerSchemaStorage serverSchemaStorage();

	NodeIndexHandlerImpl nodeContainerIndexHandler();

	NodeMigrationImpl nodeMigrationHandler();

	BranchMigrationImpl branchMigrationHandler();

	MicronodeMigrationImpl micronodeMigrationHandler();

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

	WriteLock globalLock();

	RoleCrudHandler roleCrudHandler();

	List<ConsistencyCheck> consistencyChecks();

	interface Builder {
		Builder configuration(MeshOptions options);
		Builder mesh(Mesh mesh);
		Builder searchProviderType(@Nullable SearchProviderType type);
		MeshComponent build();
	}
}
