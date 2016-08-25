package com.gentics.mesh.dagger;

import javax.inject.Singleton;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.verticle.auth.AuthenticationVerticle;
import com.gentics.mesh.core.verticle.group.GroupVerticle;
import com.gentics.mesh.core.verticle.microschema.MicroschemaVerticle;
import com.gentics.mesh.core.verticle.microschema.ProjectMicroschemaVerticle;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.core.verticle.project.ProjectVerticle;
import com.gentics.mesh.core.verticle.release.ReleaseVerticle;
import com.gentics.mesh.core.verticle.role.RoleVerticle;
import com.gentics.mesh.core.verticle.schema.ProjectSchemaVerticle;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyVerticle;
import com.gentics.mesh.core.verticle.user.UserVerticle;
import com.gentics.mesh.demo.TestDataProvider;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.impl.DummySearchProvider;
import com.gentics.mesh.search.impl.DummySearchProviderModule;

import dagger.Component;

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

}