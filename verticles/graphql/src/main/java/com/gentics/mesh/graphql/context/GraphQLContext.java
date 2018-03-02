package com.gentics.mesh.graphql.context;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.error.PermissionException;

import java.util.function.Supplier;

/**
 * Extended context for GraphQL handling.
 */
public interface GraphQLContext extends InternalActionContext {

	/**
	 * Check whether at least one of the provided permissions is granted. Otherwise a failure {@link PermissionException} will be thrown.
	 * 
	 * @param vertex
	 *            Element to be checked
	 * @param permission
	 * @return Provided element will be returned if at least one of the permissions grants access
	 * @throws PermissionException
	 */
	<T extends MeshCoreVertex<?, ?>> T requiresPerm(T vertex, GraphPermission... permission);

	default <T> T getOrStore(String key, Supplier<T> supplier) {
		T value = get(key);
		if (value == null) {
			value = supplier.get();
			put(key, value);
		}
		return value;
	}
}
