package com.gentics.mesh.dagger;

import javax.inject.Singleton;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.auth.MeshAuthHandler;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.endpoint.migration.micronode.MicronodeMigrationHandler;
import com.gentics.mesh.core.endpoint.migration.node.NodeMigrationHandler;
import com.gentics.mesh.core.endpoint.migration.release.ReleaseMigrationHandler;
import com.gentics.mesh.core.endpoint.node.BinaryFieldHandler;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.verticle.job.JobWorkerVerticle;
import com.gentics.mesh.dagger.MeshComponent.Builder;
import com.gentics.mesh.dagger.module.BindModule;
import com.gentics.mesh.dagger.module.FakeConsoleModule;
import com.gentics.mesh.dagger.module.MeshModule;
import com.gentics.mesh.dagger.module.TestSearchProviderModule;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.rest.MeshLocalClientImpl;
import com.gentics.mesh.rest.RestAPIVerticle;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.search.IndexHandlerRegistry;
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

import dagger.BindsInstance;
import dagger.Component;

@Singleton
@Component(modules = { MeshModule.class, OAuth2Module.class, TestSearchProviderModule.class, BindModule.class, FakeConsoleModule.class })
public interface TestMeshComponent extends MeshComponent {

	BootstrapInitializer boot();

	Database database();

	SearchQueue searchQueue();

	BCryptPasswordEncoder passwordEncoder();

	RouterStorage routerStorage();

	BinaryStorage binaryStorage();

	default TrackingSearchProvider trackingSearchProvider() {
		return (TrackingSearchProvider) searchProvider();
	}

	MeshAuthHandler authenticationHandler();

	JobWorkerVerticle jobWorkerVerticle();

	ServerSchemaStorage serverSchemaStorage();

	NodeIndexHandler nodeContainerIndexHandler();

	NodeMigrationHandler nodeMigrationHandler();

	ReleaseMigrationHandler releaseMigrationHandler();

	MicronodeMigrationHandler micronodeMigrationHandler();

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

	@Component.Builder
	interface Builder {
		@BindsInstance
		Builder configuration(MeshOptions options);

		TestMeshComponent build();
	}
}
