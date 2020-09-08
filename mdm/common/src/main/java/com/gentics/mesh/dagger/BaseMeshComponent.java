package com.gentics.mesh.dagger;

import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.generic.UserProperties;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.index.group.GroupIndexHandler;
import com.gentics.mesh.search.index.microschema.MicroschemaIndexHandler;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
import com.gentics.mesh.search.index.project.ProjectIndexHandler;
import com.gentics.mesh.search.index.role.RoleIndexHandler;
import com.gentics.mesh.search.index.schema.SchemaIndexHandler;
import com.gentics.mesh.search.index.tag.TagIndexHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.user.UserIndexHandler;

import io.vertx.core.Vertx;

public interface BaseMeshComponent {

	MeshOptions options();

	Vertx vertx();

	// Index Handlers

	UserIndexHandler userIndexHandler();

	RoleIndexHandler roleIndexHandler();

	GroupIndexHandler groupIndexHandler();

	SchemaIndexHandler schemaContainerIndexHandler();

	MicroschemaIndexHandler microschemaContainerIndexHandler();

	TagIndexHandler tagIndexHandler();

	TagFamilyIndexHandler tagFamilyIndexHandler();

	ProjectIndexHandler projectIndexHandler();

	NodeIndexHandler nodeContainerIndexHandler();

	// Data

	PermissionProperties permissionProperties();

	UserProperties userProperties();

	// Other

}
