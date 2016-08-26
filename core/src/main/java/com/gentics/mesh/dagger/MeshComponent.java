package com.gentics.mesh.dagger;

import javax.inject.Singleton;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.verticle.auth.AuthenticationVerticle;
import com.gentics.mesh.core.verticle.user.UserVerticle;
import com.gentics.mesh.demo.TestDataProvider;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.impl.ElasticSearchProviderModule;

import dagger.Component;

@Singleton
@Component(modules = { MeshModule.class, ElasticSearchProviderModule.class })
public interface MeshComponent {

	BootstrapInitializer boot();

	Database database();

	SearchProvider searchProvider();

	UserVerticle userVerticle();

	TestDataProvider testDataProvider();

	BCryptPasswordEncoder passwordEncoder();

	RouterStorage routerStorage();

	AuthenticationVerticle authenticationVerticle();

	ServerSchemaStorage serverSchemaStorage();

	WebRootLinkReplacer webRootLinkReplacer();

	SchemaComparator schemaComparator();

	MicroschemaComparator microschemaComparator();

}
