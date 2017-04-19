package com.gentics.mesh.core.data;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.DateUtils;

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
 * <img src= "http://getmesh.io/docs/javadoc/cypher/com.gentics.mesh.core.data.impl.UserImpl.jpg" alt="">
 * </p>
 */
public interface User extends MeshCoreVertex<UserResponse, User>, ReferenceableElement<UserReference>, UserTrackingVertex, IndexableElement {

	/**
	 * Type Value: {@value #TYPE}
	 */
	static String TYPE = "user";

	/**
	 * API key token property name {@value #API_KEY_TOKEN_CODE}
	 */
	static String API_KEY_TOKEN_CODE = "APIKeyTokenCode";

	/**
	 * API key token timestamp property name {@value #API_KEY_TOKEN_CODE_ISSUE_TIMESTAMP}
	 */
	static String API_KEY_TOKEN_CODE_ISSUE_TIMESTAMP = "APIKeyTokenCodeTimestamp";

	@Override
	default String getType() {
		return User.TYPE;
	}

	/**
	 * Compose the index type for the user index.
	 * 
	 * @return
	 */
	static String composeIndexType() {
		return TYPE.toLowerCase();
	}

	/**
	 * Compose the index name for the user index.
	 * 
	 * @return
	 */
	static String composeIndexName() {
		return TYPE.toLowerCase();
	}

	/**
	 * Compose the document id for the user documents.
	 * 
	 * @param elementUuid
	 * @return
	 */
	static String composeDocumentId(String elementUuid) {
		Objects.requireNonNull(elementUuid, "A elementUuid must be provided.");
		return elementUuid;
	}

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
	 * @return Fluent API
	 */
	User setUsername(String string);

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
	 * @return Fluent API
	 */
	User setEmailAddress(String email);

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
	 * @return Fluent API
	 */
	User setLastname(String lastname);

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
	 * @return Fluent API
	 */
	User setFirstname(String firstname);

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
	 * @return Fluent API
	 */
	// TODO change this to an async call since hashing of the password is
	// blocking
	User setPasswordHash(String hash);

	/**
	 * Set the plaintext password. Internally the password string will be hashed and the password hash will be set.
	 * 
	 * @param password
	 * @return Fluent API
	 */
	User setPassword(String password);

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
	User setReferencedNode(Node node);

	/**
	 * Return the permission info object for the given vertex.
	 * 
	 * @param vertex
	 * @return
	 */
	PermissionInfo getPermissionInfo(MeshVertex vertex);

	/**
	 * Return a set of permissions which the user got for the given vertex.
	 * 
	 * @param vertex
	 * @return
	 */
	Set<GraphPermission> getPermissions(MeshVertex vertex);

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
	User addCRUDPermissionOnRole(MeshVertex sourceNode, GraphPermission permission, MeshVertex targetNode);

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
	User addPermissionsOnRole(MeshVertex sourceNode, GraphPermission permission, MeshVertex targetNode, GraphPermission... toGrant);

	/**
	 * Inherit permissions egdes from the source node and assign those permissions to the target node.
	 * 
	 * @param sourceNode
	 * @param targetNode
	 */
	User inheritRolePermissions(MeshVertex sourceNode, MeshVertex targetNode);

	/**
	 * Return a page of groups which the user was assigned to.
	 * 
	 * @param user
	 * @param params
	 * @return
	 */
	Page<? extends Group> getGroups(User user, PagingParameters params);

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
	User addGroup(Group group);

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
	User disable();

	/**
	 * Check whether the user is enabled.
	 * 
	 * @return
	 */
	boolean isEnabled();

	/**
	 * Enable the user.
	 */
	User enable();

	/**
	 * Disable the user and remove him from all groups
	 */
	User deactivate();

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

	/**
	 * Check whether the user is allowed to read the given node. Internally this check the currently configured version scope and check for
	 * {@link GraphPermission#READ_PERM} or {@link GraphPermission#READ_PUBLISHED_PERM}.
	 * 
	 * @param ac
	 * @param node
	 * @return
	 */
	boolean canReadNode(InternalActionContext ac, Node node);

	/**
	 * Set the reset token for the user.
	 * 
	 * @param token
	 * @return Fluent API
	 */
	User setResetToken(String token);

	/**
	 * Return the currently stored reset token.
	 * 
	 * @return Token or null if no token has been set or if the token has been used up
	 */
	String getResetToken();

	/**
	 * Return the timestamp on which the token code was issued.
	 * 
	 * @return
	 */
	Long getResetTokenIssueTimestamp();

	/**
	 * Set the token code issue timestamp. This is used to influence the token expire moment.
	 * 
	 * @param timestamp
	 * @return
	 */
	User setResetTokenIssueTimestamp(Long timestamp);

	/**
	 * Invalidate the reset token.
	 * 
	 * @return Fluent API
	 */
	default User invalidateResetToken() {
		setResetToken(null);
		setResetTokenIssueTimestamp(null);
		return this;
	}

	/**
	 * Check whether the given token code is valid. This method will also clear the token code if the token has expired.
	 * 
	 * @param token
	 *            Token Code
	 * @param maxTokenAgeMins
	 *            maximum allowed token age in minutes
	 * @return
	 */
	default boolean isResetTokenValid(String token, int maxTokenAgeMins) {
		if (token == null) {
			return false;
		}
		long maxTokenAge = 1000 * 60 * maxTokenAgeMins;
		long tokenAge = System.currentTimeMillis() - getResetTokenIssueTimestamp();
		boolean isExpired = tokenAge > maxTokenAge;
		boolean isTokenMatch = token.equals(getResetToken());

		if (isTokenMatch && isExpired) {
			invalidateResetToken();
			return false;
		}
		return isTokenMatch && !isExpired;
	}

	/**
	 * Return the currently stored API key token code.
	 * 
	 * @return API key token code or null if no code has yet been generated.
	 */
	default String getAPIKeyTokenCode() {
		return getProperty(API_KEY_TOKEN_CODE);
	}

	/**
	 * Set the user API key token code.
	 * 
	 * @param code
	 * @return Fluent API
	 */
	default User setAPIKeyTokenCode(String code) {
		setProperty(API_KEY_TOKEN_CODE, code);
		return this;
	}

	/**
	 * Return the timestamp when the api key token code was last issued.
	 * 
	 * @return
	 */
	default Long getAPIKeyTokenCodeIssueTimestamp() {
		return getProperty(API_KEY_TOKEN_CODE_ISSUE_TIMESTAMP);
	}

	/**
	 * Set the API Key token code issue timestamp to the current time.
	 * 
	 * @return Fluent API
	 */
	default User setAPIKeyTokenCodeIssueTimestamp() {
		setAPIKeyTokenCodeIssueTimestamp(System.currentTimeMillis());
		return this;
	}

	/**
	 * Set the API key token code issue timestamp.
	 * 
	 * @param timestamp
	 * @return Fluent API
	 */
	default User setAPIKeyTokenCodeIssueTimestamp(Long timestamp) {
		setProperty(API_KEY_TOKEN_CODE_ISSUE_TIMESTAMP, timestamp);
		return this;
	}

	/**
	 * Return the api key token issue date.
	 * 
	 * @return ISO8601 formatted date or null if the date has not yet been set
	 */
	default String getAPIKeyTokenCodeIssueDate() {
		Long timestamp = getAPIKeyTokenCodeIssueTimestamp();
		if (timestamp == null) {
			return null;
		}
		return DateUtils.toISO8601(timestamp, System.currentTimeMillis());
	}

	/**
	 * Reset the API key token code and issue timestamp.
	 */
	default void deleteAPIKeyTokenCode() {
		setProperty(API_KEY_TOKEN_CODE, null);
		setProperty(API_KEY_TOKEN_CODE_ISSUE_TIMESTAMP, null);
	}

}
