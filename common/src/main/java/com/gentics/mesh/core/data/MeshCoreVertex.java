package com.gentics.mesh.core.data;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * A mesh core vertex is an vertex which can be manipulated via CRUD by the user. Thus this interface provides various methods that are needed to interact with
 * such vertices.
 * 
 * @param <R>
 *            Rest model class of the core vertex
 * @param <V>
 *            The core vertex itself
 */
public interface MeshCoreVertex<R extends RestModel, V extends MeshCoreVertex<R, V>> extends MeshVertex, TransformableElement<R> {

	/**
	 * Update the vertex using the action context information.
	 * 
	 * @param ac
	 * @param batch
	 *            Batch to which entries will be added in order to update the search index.
	 */
	V update(InternalActionContext ac, SearchQueueBatch batch);

	/**
	 * Return the type info of the element.
	 * 
	 * @return
	 */
	TypeInfo getTypeInfo();

	/**
	 * Add common fields to the given rest model object. The method will add common files like creator, editor, uuid, permissions, edited, created.
	 * 
	 * @param model
	 * @param ac
	 */
	void fillCommonRestFields(InternalActionContext ac, GenericRestResponse model);

	/**
	 * Add role permissions to given rest model object.
	 * 
	 * @param ac
	 * @param model
	 */
	void setRolePermissions(InternalActionContext ac, GenericRestResponse model);

	/**
	 * Return an iterable for all roles which grant the permission to the element.
	 * 
	 * @param perm
	 * @return
	 */
	Iterable<? extends Role> getRolesWithPerm(GraphPermission perm);

	/**
	 * Method which is being invoked once the element has been created.
	 */
	void onCreated();

	/**
	 * Method which is being invoked once the element has been updated.
	 */
	void onUpdated();

}
