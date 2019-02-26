package com.gentics.mesh.graphql.context.impl;

import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.graphql.context.GraphQLContext;
import io.vertx.ext.web.RoutingContext;

import static com.gentics.mesh.core.rest.error.Errors.missingPerm;

/**
 * @see GraphQLContext
 */
public class GraphQLContextImpl extends InternalRoutingActionContextImpl implements GraphQLContext {

	public GraphQLContextImpl(RoutingContext rc) {
		super(rc);
	}

	@Override
	public <T extends MeshCoreVertex<?, ?>> T requiresPerm(T vertex, GraphPermission... permission) {
		for (GraphPermission perm : permission) {
			if (getUser().hasPermission(vertex, perm)) {
				return vertex;
			}
		}
		throw missingPerm(vertex.getTypeInfo().getType().name().toLowerCase(), vertex.getUuid());
	}

}
