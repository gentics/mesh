package com.gentics.mesh.dagger;

import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.Vertx;

public interface BaseMeshComponent {

	MeshOptions options();

	Vertx vertx();

	PermissionProperties permissionProperties();

}
