package com.gentics.mesh.core.data.root;

import java.util.List;
import java.util.Stack;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.util.InvalidArgumentException;

import rx.Single;

/**
 * A root vertex is an aggregation vertex that is used to aggregate various basic elements such as users, nodes, groups.
 */
public interface RootVertex<T extends MeshCoreVertex<? extends RestModel, T>> extends MeshVertex {

	/**
	 * Return a list of all elements.
	 * 
	 * @return
	 */
	List<? extends T> findAll();

	/**
	 * Find the element with the given name.
	 * 
	 * @param name
	 * @return
	 */
	Single<T> findByName(String name);

	/**
	 * Load the object by name and check the given permissions.
	 * 
	 * @param ac
	 *            Context to be used in order to check user permissions
	 * @param name
	 *            Name of the object that should be loaded
	 * @param perm
	 *            Permission that must be granted in order to load the object
	 * 
	 * @return
	 */
	Single<T> findByName(InternalActionContext ac, String projectName, GraphPermission perm);

	/**
	 * Find the element with the given uuid.
	 * 
	 * @param uuid
	 *            Uuid of the element to be located
	 * @return Observable which may emit the located element
	 */
	Single<T> findByUuid(String uuid);

	/**
	 * Find the element with the given uuid.
	 * 
	 * @param uuid
	 *            Uuid of the element to be located
	 * @return
	 */
	T findByUuidSync(String uuid);

	/**
	 * Find the visible elements and return a paged result.
	 * 
	 * @param ac
	 *            action context
	 * @param pagingInfo
	 *            Paging information object that contains page options.
	 * 
	 * @return
	 * @throws InvalidArgumentException
	 *             if the paging options are malformed.
	 */
	PageImpl<? extends T> findAll(InternalActionContext ac, PagingParameters pagingInfo) throws InvalidArgumentException;

	/**
	 * Resolve the given stack to the vertex.
	 * 
	 * @param stack
	 * @return
	 */
	Single<? extends MeshVertex> resolveToElement(Stack<String> stack);

	/**
	 * Create a new object within this aggregation vertex.
	 * 
	 * @param ac
	 */
	Single<T> create(InternalActionContext ac);

	/**
	 * Load the object by uuid and check the given permissions.
	 * 
	 * @param ac
	 *            Context to be used in order to check user permissions
	 * @param uuid
	 *            Uuid of the object that should be loaded
	 * @param perm
	 *            Permission that must be granted in order to load the object
	 */
	Single<T> loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm);

	/**
	 * Load the object by uuid and check the given permissions.
	 * 
	 * @param ac
	 * @param uuid
	 * @param perm
	 * @return
	 */
	T loadObjectByUuidSync(InternalActionContext ac, String uuid, GraphPermission perm);

	/**
	 * Add the given item to the this root vertex.
	 * 
	 * @param item
	 */
	void addItem(T item);

	/**
	 * Remove the given item from this root vertex.
	 * 
	 * @param item
	 */
	void removeItem(T item);

	/**
	 * Return the label for the item edges.
	 * 
	 * @return
	 */
	String getRootLabel();

	/**
	 * Return the ferma graph persistance class for the items of the root vertex. (eg. NodeImpl, TagImpl...)
	 * 
	 * @return
	 */
	Class<? extends T> getPersistanceClass();

}
