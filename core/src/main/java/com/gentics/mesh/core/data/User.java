package com.gentics.mesh.core.data;

import java.util.List;
import java.util.Set;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;

import rx.Single;

/**
 * The User Domain Model interface.
 *
 * <pre>
* {@code
* 	(u:UserImpl)-[r1:HAS_USER]->(ur:UserRootImpl)
* 	(u)-[r2:HAS_USER]->(g:GroupImpl)
 	(g)<-[r3:HAS_ROLE]-(r:RoleImpl)
* }
 * </pre>
 *
 * <p>
 * <img src="http://getmesh.io/docs/javadoc/cypher/com.gentics.mesh.core.data.impl.UserImpl.jpg" alt="">
 * </p>
 */
public interface User extends MeshCoreVertex<UserResponse, User>, ReferenceableElement<UserReference>, UserTrackingVertex {

	/**
	 * Type Value: {@value #TYPE}
	 */
	public static final String TYPE = "user";

	/**
	 * Return the username.
	 * 
	 * @return Username
	 */
	String getUsername();

	/**
	 * Set the username.
	 * 
	 * @param string
	 *            Username
	 */
	void setUsername(String string);

	/**
	 * Return the email address.
	 * 
	 * @return Email address or null when no email address was set
	 */
	String getEmailAddress();

	/**
	 * Set the email address.
	 * 
	 * @param email
	 *            Email address
	 */
	void setEmailAddress(String email);

	/**
	 * Return the lastname.
	 * 
	 * @return Lastname
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
	 * @return Firstname
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
	 * @return Password hash
	 */
	String getPasswordHash();

	/**
	 * Set the password hash.
	 * 
	 * @param hash
	 *            Password hash
	 */
	// TODO change this to an async call since hashing of the password is blocking
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

	/**
	 * Return an array of human readable permissions for the given vertex.
	 * 
	 * @param ac
	 * @param vertex
	 * @return
	 * @deprecated Use {@link #getPermissionNames(InternalActionContext, MeshVertex)} instead.
	 * 
	 */
	@Deprecated
	String[] getPermissionNames(InternalActionContext ac, MeshVertex vertex);

	/**
	 * Collect the permissions names for the given vertex.
	 * 
	 * @param ac
	 * @param node
	 */
	Single<List<String>> getPermissionNamesAsync(InternalActionContext ac, MeshVertex node);

	/**
	 * Return a set of permissions which the user got for the given vertex.
	 * 
	 * @param ac
	 *            The action context data map will be used to quickly lookup already determined permissions.
	 * @param vertex
	 * @return
	 */
	Set<GraphPermission> getPermissions(InternalActionContext ac, MeshVertex vertex);

	/**
	 * This method will set CRUD permissions to the target node for all roles that would grant the given permission on the node. The method is most often used
	 * to assign CRUD permissions on newly created elements. Example for adding CRUD permissions on a newly created project: The method will first determine the
	 * list of roles that would initially enable you to create a new project. It will do so by examining the projectRoot node. After this step the CRUD
	 * permissions will be added to the newly created project and the found roles. In this case the call would look like this:
	 * addCRUDPermissionOnRole(projectRoot, Permission.CREATE_PERM, newlyCreatedProject); This method will ensure that all users/roles that would be able to
	 * create an element will also be able to CRUD it even when the creator of the element was only assigned to one of the enabling roles. Additionally the
	 * permissions of the source node are inherited by the target node. All permissions between the source node and roles are copied to the target node.
	 * 
	 * @param sourceNode
	 *            Node that will be checked against to find all roles that would grant the given permission.
	 * @param permission
	 *            Permission that is used in conjunction with the node to determine the list of affected roles.
	 * @param targetNode
	 *            Node to which the CRUD permissions will be assigned.
	 */
	void addCRUDPermissionOnRole(MeshVertex sourceNode, GraphPermission permission, MeshVertex targetNode);

	/**
	 * This method adds additional permissions to the target node. The roles are selected like in method
	 * {@link #addCRUDPermissionOnRole(MeshVertex, GraphPermission, MeshVertex)} .
	 * 
	 * @param sourceNode
	 *            Node that will be checked
	 * @param permission
	 *            checked permission
	 * @param targetNode
	 *            target node
	 * @param toGrant
	 *            permissions to grant
	 */
	void addPermissionsOnRole(MeshVertex sourceNode, GraphPermission permission, MeshVertex targetNode, GraphPermission... toGrant);

	/**
	 * Inherit permissions egdes from the source node and assign those permissions to the target node.
	 * 
	 * @param sourceNode
	 * @param targetNode
	 */
	void inheritRolePermissions(MeshVertex sourceNode, MeshVertex targetNode);

	/**
	 * Return a list of groups to which the user was assigned.
	 * 
	 * @return
	 */
	List<? extends Group> getGroups();

	/**
	 * Add the user to the given group.
	 * 
	 * @param group
	 */
	void addGroup(Group group);

	/**
	 * Return a list of roles which belong to this user. Internally this will fetch all groups of the user and collect the assigned roles.
	 * 
	 * @return
	 */
	List<? extends Role> getRoles();

	/**
	 * Return a list of roles that belong to the user. Internally this will check the user role shortcut edge.
	 * 
	 * @return
	 */
	List<? extends Role> getRolesViaShortcut();

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
	 * Check whether the user has the given permission on the given element.
	 * 
	 * @param element
	 * @param permission
	 * @return
	 */
	boolean hasPermission(MeshVertex element, GraphPermission permission);

	/**
	 * Check whether the user has the given permission on the element with the given id.
	 * 
	 * @param elementId
	 * @param permission
	 * @return
	 */
	boolean hasPermissionForId(Object elementId, GraphPermission permission);

	/**
	 * Check whether the admin role was assigned to the user.
	 * 
	 * @return
	 */
	boolean hasAdminRole();

}
