package com.gentics.mesh.dagger;

import javax.inject.Provider;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.cache.ProjectBranchNameCache;
import com.gentics.mesh.cache.ProjectNameCache;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.generic.UserProperties;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.migration.BranchMigration;
import com.gentics.mesh.core.migration.MicronodeMigration;
import com.gentics.mesh.core.migration.NodeMigration;
import com.gentics.mesh.core.project.maintenance.ProjectVersionPurgeHandler;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.manager.MeshPluginManager;
import com.gentics.mesh.rest.MeshLocalClient;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.group.GroupIndexHandler;
import com.gentics.mesh.search.index.microschema.MicroschemaIndexHandler;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
import com.gentics.mesh.search.index.project.ProjectIndexHandler;
import com.gentics.mesh.search.index.role.RoleIndexHandler;
import com.gentics.mesh.search.index.schema.SchemaIndexHandler;
import com.gentics.mesh.search.index.tag.TagIndexHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.user.UserIndexHandler;
import com.gentics.mesh.storage.BinaryStorage;
import com.gentics.mesh.storage.LocalBinaryStorage;

import io.vertx.core.Vertx;

public interface BaseMeshComponent {

	MeshOptions options();

	Vertx vertx();

	PasswordEncoder passwordEncoder();

	SchemaComparator schemaComparator();

	// Data

	PermissionProperties permissionProperties();

	UserProperties userProperties();

	// Caches

	PermissionCache permissionCache();

	ProjectBranchNameCache branchCache();

	ProjectNameCache projectNameCache();

	// Plugin

	PluginEnvironment pluginEnv();

	// Other

	MeshPluginManager pluginManager();

	MeshLocalClient meshLocalClientImpl();

	ImageManipulator imageManipulator();

	LocalBinaryStorage localBinaryStorage();

	BinaryStorage binaryStorage();

	Provider<BulkActionContext> bulkProvider();

	WebRootLinkReplacer webRootLinkReplacer();

	ProjectVersionPurgeHandler projectVersionPurgeHandler();

	ServerSchemaStorage serverSchemaStorage();

	// Search

	SearchProvider searchProvider();

	UserIndexHandler userIndexHandler();

	RoleIndexHandler roleIndexHandler();

	GroupIndexHandler groupIndexHandler();

	SchemaIndexHandler schemaContainerIndexHandler();

	MicroschemaIndexHandler microschemaContainerIndexHandler();

	TagIndexHandler tagIndexHandler();

	TagFamilyIndexHandler tagFamilyIndexHandler();

	ProjectIndexHandler projectIndexHandler();

	NodeIndexHandler nodeContainerIndexHandler();

	// Migration

	NodeMigration nodeMigrationHandler();

	BranchMigration branchMigrationHandler();

	MicronodeMigration micronodeMigrationHandler();

	// REST

	WriteLock globalLock();
}
