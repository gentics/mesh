package com.gentics.mesh.dagger;

import javax.inject.Singleton;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.node.handler.NodeMigrationHandler;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.verticle.admin.AdminVerticle;
import com.gentics.mesh.core.verticle.admin.RestInfoVerticle;
import com.gentics.mesh.core.verticle.auth.AuthenticationVerticle;
import com.gentics.mesh.core.verticle.eventbus.EventbusVerticle;
import com.gentics.mesh.core.verticle.group.GroupVerticle;
import com.gentics.mesh.core.verticle.microschema.MicroschemaVerticle;
import com.gentics.mesh.core.verticle.microschema.ProjectMicroschemaVerticle;
import com.gentics.mesh.core.verticle.navroot.NavRootVerticle;
import com.gentics.mesh.core.verticle.node.NodeFieldAPIHandler;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.core.verticle.project.ProjectVerticle;
import com.gentics.mesh.core.verticle.release.ReleaseVerticle;
import com.gentics.mesh.core.verticle.role.RoleVerticle;
import com.gentics.mesh.core.verticle.schema.ProjectSchemaVerticle;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyVerticle;
import com.gentics.mesh.core.verticle.user.UserVerticle;
import com.gentics.mesh.core.verticle.utility.UtilityVerticle;
import com.gentics.mesh.core.verticle.webroot.WebRootVerticle;
import com.gentics.mesh.demo.TestDataProvider;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.rest.MeshLocalClientImpl;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.ProjectSearchVerticle;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.SearchVerticle;
import com.gentics.mesh.search.impl.DummySearchProvider;
import com.gentics.mesh.search.impl.DummySearchProviderModule;
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
import io.vertx.core.AbstractVerticle;

@Singleton
@Component(modules = { MeshModule.class, DummySearchProviderModule.class })
public interface TestMeshComponent extends MeshComponent {

	BootstrapInitializer boot();

	Database database();

	SearchProvider searchProvider();

	UserVerticle userVerticle();

	TestDataProvider testDataProvider();

	BCryptPasswordEncoder passwordEncoder();

	RouterStorage routerStorage();

	default DummySearchProvider dummySearchProvider() {
		return (DummySearchProvider) searchProvider();
	}

	AuthenticationVerticle authenticationVerticle();

	ProjectVerticle projectVerticle();

	NodeVerticle nodeVerticle();

	RoleVerticle roleVerticle();

	GroupVerticle groupVerticle();

	TagFamilyVerticle tagFamilyVerticle();

	ReleaseVerticle releaseVerticle();

	SchemaVerticle schemaVerticle();

	MicroschemaVerticle microschemaVerticle();

	ProjectSchemaVerticle projectSchemaVerticle();

	ProjectMicroschemaVerticle projectMicroschemaVerticle();

	EventbusVerticle eventbusVerticle();

	AdminVerticle adminVerticle();

	NodeMigrationVerticle nodeMigrationVerticle();

	ServerSchemaStorage serverSchemaStorage();

	NodeIndexHandler nodeIndexHandler();

	SearchVerticle searchVerticle();

	NodeMigrationHandler nodeMigrationHandler();

	RestInfoVerticle restInfoVerticle();

	MeshLocalClientImpl meshLocalClientImpl();

	WebRootLinkReplacer webRootLinkReplacer();

	ProjectSearchVerticle projectSearchVerticle();

	IndexHandlerRegistry indexHandlerRegistry();

	WebRootVerticle webrootVerticle();

	NavRootVerticle navRootVerticle();

	UtilityVerticle utilityVerticle();

	ProjectIndexHandler projectIndexHandler();

	UserIndexHandler userIndexHandler();

	RoleIndexHandler roleIndexHandler();

	GroupIndexHandler groupIndexHandler();

	SchemaContainerIndexHandler schemaContainerIndexHandler();

	MicroschemaContainerIndexHandler microschemaContainerIndexHandler();

	TagIndexHandler tagIndexHandler();

	TagFamilyIndexHandler tagFamilyIndexHandler();

	NodeFieldAPIHandler nodeFieldAPIHandler();

	MicroschemaComparator microschemaComparator();

}