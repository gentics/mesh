package com.gentics.mesh.core.data.root;

import java.util.List;
import java.util.Stack;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.util.InvalidArgumentException;

import rx.Observable;

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
	Observable<T> findByName(String name);

	/**
	 * Find the element with the given uuid and call the result handler.
	 * 
	 * @param uuid
	 *            Uuid of the element to be located.
	 * @return this root vertex - fluent API
	 */
	Observable<T> findByUuid(String uuid);

	/**
	 * Find the visible elements and return a paged result.
	 * 
	 * @param requestUser
	 *            User that is used to check read permissions against.
	 * @param pagingInfo
	 *            Paging information object that contains page options.
	 * @return
	 * @throws InvalidArgumentException
	 *             if the paging options are malformed.
	 */
	PageImpl<? extends T> findAll(MeshAuthUser requestUser, PagingParameter pagingInfo) throws InvalidArgumentException;

	/**
	 * Resolve the given stack to the vertex.
	 * 
	 * @param stack
	 * @return
	 */
	Observable<? extends MeshVertex> resolveToElement(Stack<String> stack);

	/**
	 * Create a new object within this aggregation vertex.
	 * 
	 * @param ac
	 */
	Observable<T> create(InternalActionContext ac);

	/**
	 * Load the object by uuid and check the given permission.
	 * 
	 * @param ac
	 *            Context to be used in order to check user permissions
	 * @param uuid
	 *            Uuid of the object that should be loaded
	 * @param perm
	 *            Permission that must be granted in order to load the object
	 * @param root
	 *            Aggregation root vertex that should be used to find the element
	 */
	Observable<T> loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm);

	/**
	 * Load object by extracting the uuid from the given uuid parameter name using the action context.
	 * 
	 * @param ac
	 * @param uuidParameterName
	 * @param perm
	 * @return
	 */
	Observable<T> loadObject(InternalActionContext ac, String uuidParameterName, GraphPermission perm);

}
