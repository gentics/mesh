package com.gentics.mesh.core.data;

import java.util.Set;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.relationship.Permission;
import com.tinkerpop.blueprints.Vertex;

public interface MeshVertex extends MeshElement {

	Vertex getVertex();

	MeshVertexImpl getImpl();

	void delete();

	void applyPermissions(Role role, boolean recursive, Set<Permission> permissionsToGrant, Set<Permission> permissionsToRevoke);
}
