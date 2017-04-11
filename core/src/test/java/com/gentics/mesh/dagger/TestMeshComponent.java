package com.gentics.mesh.dagger;

import javax.inject.Singleton;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.auth.MeshAuthHandler;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.node.handler.NodeMigrationHandler;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.verticle.node.BinaryFieldHandler;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.dagger.module.ExtraModule;
import com.gentics.mesh.dagger.module.FakeConsoleModule;
import com.gentics.mesh.dagger.module.MeshModule;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.rest.MeshLocalClientImpl;
import com.gentics.mesh.rest.RestAPIVerticle;
import com.gentics.mesh.search.DummySearchProvider;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.group.GroupIndexHandler;
import com.gentics.mesh.search.index.microschema.MicroschemaContainerIndexHandler;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
import com.gentics.mesh.search.index.project.ProjectIndexHandler;
import com.gentics.mesh.search.index.role.RoleIndexHandler;
import com.gentics.mesh.search.index.schema.SchemaContainerIndexHandler;
import com.gentics.mesh.search.index.tag.TagIndexHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.user.UserIndexHandler;

import dagger.Component;

@Singleton
@Component(modules = { MeshModule.class, ExtraModule.class, FakeConsoleModule.class })
public interface TestMeshComponent extends MeshComponent {

	BootstrapInitializer boot();

	Database database();

	SearchQueue searchQueue();

	SearchProvider searchProvider();

	BCryptPasswordEncoder passwordEncoder();

	RouterStorage routerStorage();

	default DummySearchProvider dummySearchProvider() {
		return (DummySearchProvider) searchProvider();
	}

	MeshAuthHandler authenticationHandler();

	NodeMigrationVerticle nodeMigrationVerticle();

	ServerSchemaStorage serverSchemaStorage();

	NodeIndexHandler nodeContainerIndexHandler();

	NodeMigrationHandler nodeMigrationHandler();

	MeshLocalClientImpl meshLocalClientImpl();

	WebRootLinkReplacer webRootLinkReplacer();

	IndexHandlerRegistry indexHandlerRegistry();

	ProjectIndexHandler projectIndexHandler();

	UserIndexHandler userIndexHandler();

	RoleIndexHandler roleIndexHandler();

	GroupIndexHandler groupIndexHandler();

	SchemaContainerIndexHandler schemaContainerIndexHandler();

	MicroschemaContainerIndexHandler microschemaContainerIndexHandler();

	TagIndexHandler tagIndexHandler();

	TagFamilyIndexHandler tagFamilyIndexHandler();

	BinaryFieldHandler nodeFieldAPIHandler();

	ImageManipulator imageManipulator();

	SchemaComparator schemaComparator();

	RestAPIVerticle restApiVerticle();
}
