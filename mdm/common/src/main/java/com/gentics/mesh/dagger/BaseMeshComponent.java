package com.gentics.mesh.dagger;

import javax.inject.Provider;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.cache.ProjectBranchNameCache;
import com.gentics.mesh.cache.ProjectNameCache;
import com.gentics.mesh.cache.WebrootPathCache;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.UserProperties;
import com.gentics.mesh.core.data.page.PageTransformer;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.role.RoleCrudHandler;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.migration.BranchMigration;
import com.gentics.mesh.core.migration.MicronodeMigration;
import com.gentics.mesh.core.migration.NodeMigration;
import com.gentics.mesh.core.project.maintenance.ProjectVersionPurgeHandler;
import com.gentics.mesh.core.search.index.node.NodeIndexHandler;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.core.verticle.job.JobWorkerVerticle;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.manager.MeshPluginManager;
import com.gentics.mesh.rest.MeshLocalClient;
import com.gentics.mesh.router.EndpointRegistry;
import com.gentics.mesh.router.RouterStorageRegistry;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.TrackingSearchProvider;
import com.gentics.mesh.search.index.group.GroupIndexHandler;
import com.gentics.mesh.search.index.microschema.MicroschemaIndexHandler;
import com.gentics.mesh.search.index.project.ProjectIndexHandler;
import com.gentics.mesh.search.index.role.RoleIndexHandler;
import com.gentics.mesh.search.index.schema.SchemaIndexHandler;
import com.gentics.mesh.search.index.tag.TagIndexHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.user.UserIndexHandler;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.data.storage.LocalBinaryStorage;
import com.gentics.mesh.core.data.storage.S3BinaryStorage;

import io.vertx.core.Vertx;

/**
 * Dagger interface for the central mesh components. The method allow quick access to elements outside of the dagger di scope.
 */
public interface BaseMeshComponent {

	@Getter
	Database database();

	@Getter
	MeshOptions options();

	@Getter
	Vertx vertx();

	@Getter
	PasswordEncoder passwordEncoder();

	@Getter
	SchemaComparator schemaComparator();

	@Getter
	MicroschemaComparator microschemaComparator();

	// Data

	@Getter
	UserProperties userProperties();

	// Caches
	@Getter
	WebrootPathCache pathCache();

	@Getter
	PermissionCache permissionCache();

	@Getter
	ProjectBranchNameCache branchCache();

	@Getter
	ProjectNameCache projectNameCache();

	@Getter
	PageTransformer pageTransformer();

	// Plugin
	@Getter
	PluginEnvironment pluginEnv();

	// Other
	@Getter
	MeshPluginManager pluginManager();

	@Getter
	MeshLocalClient meshLocalClientImpl();

	@Getter
	ImageManipulator imageManipulator();

	@Getter
	LocalBinaryStorage localBinaryStorage();

	@Getter
	BinaryStorage binaryStorage();

	@Getter
	S3BinaryStorage s3binaryStorage();

	@Getter
	Provider<BulkActionContext> bulkProvider();

	@Getter
	WebRootLinkReplacer webRootLinkReplacer();

	@Getter
	ProjectVersionPurgeHandler projectVersionPurgeHandler();

	@Getter
	ServerSchemaStorage serverSchemaStorage();

	@Getter
	Provider<EventQueueBatch> batchProvider();

	// Search
	@Getter
	SearchProvider searchProvider();

	@Getter
	UserIndexHandler userIndexHandler();

	@Getter
	RoleIndexHandler roleIndexHandler();

	@Getter
	GroupIndexHandler groupIndexHandler();

	@Getter
	SchemaIndexHandler schemaContainerIndexHandler();

	@Getter
	MicroschemaIndexHandler microschemaContainerIndexHandler();

	@Getter
	TagIndexHandler tagIndexHandler();

	@Getter
	TagFamilyIndexHandler tagFamilyIndexHandler();

	@Getter
	ProjectIndexHandler projectIndexHandler();

	@Getter
	NodeIndexHandler nodeContainerIndexHandler();

	@Getter
	IndexHandlerRegistry indexHandlerRegistry();

	@Getter
	default TrackingSearchProvider trackingSearchProvider() {
		return (TrackingSearchProvider) searchProvider();
	}

	// Migration
	@Getter
	NodeMigration nodeMigrationHandler();

	@Getter
	BranchMigration branchMigrationHandler();

	@Getter
	MicronodeMigration micronodeMigrationHandler();

	@Getter
	JobWorkerVerticle jobWorkerVerticle();

	// REST
	@Getter
	RoleCrudHandler roleCrudHandler();

	@Getter
	EndpointRegistry endpointRegistry();

	@Getter
	RouterStorageRegistry routerStorageRegistry();

	@Getter
	WriteLock globalLock();
}
