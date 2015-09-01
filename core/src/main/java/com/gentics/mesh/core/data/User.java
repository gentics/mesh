package com.gentics.mesh.core.data;

import java.util.List;
import java.util.Set;

import com.gentics.mesh.core.data.impl.GenericVertexImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;

public interface User extends GenericVertex<UserResponse> , NamedVertex, IndexedVertex {

	public static final String TYPE = "user";

	/**
	 * Return the username.
	 * 
	 * @return
	 */
	String getUsername();

	/**
	 * Set the username.
	 * 
	 * @param string
	 */
	void setUsername(String string);

	/**
	 * Return the email address.
	 * 
	 * @return
	 */
	String getEmailAddress();

	/**
	 * Set the email address.
	 * 
	 * @param email
	 */
	void setEmailAddress(String email);

	/**
	 * Return the lastname.
	 * 
	 * @return
	 */
	String getLastname();

	/**
	 * Set the lastname.
	 * 
	 * @param lastname
	 */
	void setLastname(String lastname);

	/**
	 * Return the firstname.
	 * 
	 * @return
	 */
	String getFirstname();

	/**
	 * Set the lastname.
	 * 
	 * @param firstname
	 */
	void setFirstname(String firstname);

	/**
	 * Return the password hash.
	 * 
	 * @return
	 */
	String getPasswordHash();

	/**
	 * Set the password hash.
	 * 
	 * @param hash
	 */
	void setPasswordHash(String hash);

	/**
	 * Set the plaintext password. Internally the password string will be hashed and the password hash will be set.
	 * 
	 * @param password
	 */
	void setPassword(String password);

	/**
	 * Return the referenced node which was assigned to the user.
	 * 
	 * @return Referenced node or null when no node was assigned to the user.
	 */
	Node getReferencedNode();

	/**
	 * Set the referenced node.
	 * 
	 * @param node
	 */
	void setReferencedNode(Node node);

	boolean hasPermission(MeshVertex vertex, GraphPermission permission);

	String[] getPermissionNames(MeshVertex vertex);

	Set<GraphPermission> getPermissions(MeshVertex node);

	/**
	 * This method will set CRUD permissions to the target node for all roles that would grant the given permission on the node. The method is most often used
	 * to assign CRUD permissions on newly created elements. Example for adding CRUD permissions on a newly created project: The method will first determine the
	 * list of roles that would initially enable you to create a new project. It will do so by examining the projectRoot node. After this step the CRUD
	 * permissions will be added to the newly created project and the found roles. In this case the call would look like this:
	 * addCRUDPermissionOnRole(projectRoot, Permission.CREATE_PERM, newlyCreatedProject); This method will ensure that all users/roles that would be able to
	 * create an element will also be able to CRUD it even when the creator of the element was only assigned to one of the enabling roles.
	 * 
	 * @param node
	 *            Node that will be checked against to find all roles that would grant the given permission.
	 * @param permission
	 *            Permission that is used in conjunction with the node to determine the list of affected roles.
	 * @param targetNode
	 *            Node to which the CRUD permissions will be assigned.
	 */
	void addCRUDPermissionOnRole(MeshVertex node, GraphPermission permission, MeshVertex targetNode);

	/**
	 * Return a list of groups to which the user was assigned.
	 * 
	 * @return
	 */
	List<? extends Group> getGroups();

	void addGroup(Group parentGroup);

	/**
	 * Return a list of roles which belong to this user. Internally this will fetch all groups of the user and collect the assigned roles.
	 * 
	 * @return
	 */
	List<? extends Role> getRoles();

	/**
	 * Disable the user.
	 */
	void disable();

	/**
	 * Check whether the user is enabled.
	 * 
	 * @return
	 */
	boolean isEnabled();

	/**
	 * Enable the user.
	 */
	void enable();

	/**
	 * Disable the user and remove him from all groups
	 */
	void deactivate();

	/**
	 * Return a user reference object for the user.
	 * 
	 * @return
	 */
	UserReference transformToUserReference();

	List<? extends GenericVertexImpl> getEditedElements();

	List<? extends GenericVertexImpl> getCreatedElements();

}
