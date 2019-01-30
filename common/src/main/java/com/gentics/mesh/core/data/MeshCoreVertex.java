package com.gentics.mesh.core.data;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.event.CreatedMeshEventModel;
import com.gentics.mesh.event.DeletedMeshEventModel;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.event.UpdatedMeshEventModel;
import com.gentics.mesh.madlmigration.TraversalResult;
import com.gentics.mesh.parameter.value.FieldsSet;

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
	 * @return true if the element was updated. Otherwise false
	 */
	boolean update(InternalActionContext ac, EventQueueBatch batch);

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
	 * @param fields
	 *            Set of fields which should be included. All fields will be included if no selective fields have been specified.
	 * @param ac
	 */
	void fillCommonRestFields(InternalActionContext ac, FieldsSet fields, GenericRestResponse model);

	/**
	 * Add role permissions to given rest model object.
	 * 
	 * @param ac
	 * @param model
	 */
	void setRolePermissions(InternalActionContext ac, GenericRestResponse model);

	/**
	 * Return a traversal result for all roles which grant the permission to the element.
	 * 
	 * @param perm
	 * @return
	 */
	TraversalResult<? extends Role> getRolesWithPerm(GraphPermission perm);

	/**
	 * Method which is being invoked once the element has been created.
	 */
	CreatedMeshEventModel onCreated();

	/**
	 * Method which is being invoked once the element has been updated.
	 * @return Created event
	 */
	UpdatedMeshEventModel onUpdated();

	/**
	 * Method which is being invoked once the element has been deleted.
	 * @return Created event
	 */
	DeletedMeshEventModel onDeleted();

}
