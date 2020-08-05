package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.vertx.core.shareddata.impl.ClusterSerializable;
import io.vertx.ext.auth.User;

/**
 * Mesh graph user which additionally implements the vertex {@link User} interface.
 */
public interface MeshAuthUser extends User, com.gentics.mesh.core.data.User, ClusterSerializable {

}
