package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.USER_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_UPDATED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import java.util.Objects;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
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
public interface User extends MeshCoreVertex<UserResponse, User>, ReferenceableElement<UserReference>, UserTrackingVertex {

	/**
	 * API token id property name {@value #API_TOKEN_ID}
	 */
	String API_TOKEN_ID = "APITokenId";

	/**
	 * API token timestamp property name {@value #API_TOKEN_ISSUE_TIMESTAMP}
	 */
	String API_TOKEN_ISSUE_TIMESTAMP = "APITokenTimestamp";

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.USER, USER_CREATED, USER_UPDATED, USER_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Compose the index name for the user index.
	 *
	 * @return
	 */
	static String composeIndexName() {
		return "user";
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
	 * Set the password hash. This will also set {@link #setForcedPasswordChange(boolean)} to false.
	 *
	 * @param hash
	 *            Password hash
	 * @return Fluent API
	 */
	User setPasswordHash(String hash);

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
	 * @return Fluent API
	 */
	User setReferencedNode(Node node);

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
	 * @return Fluent API
	 */
	User addCRUDPermissionOnRole(HasPermissions sourceNode, GraphPermission permission, MeshVertex targetNode);

	/**
	 * This method adds additional permissions to the target node. The roles are selected like in method
	 * {@link #addCRUDPermissionOnRole(HasPermissions, GraphPermission, MeshVertex)} .
	 *
	 * @param sourceNode
	 *            Node that will be checked
	 * @param permission
	 *            checked permission
	 * @param targetNode
	 *            target node
	 * @param toGrant
	 *            permissions to grant
	 * @return Fluent API
	 */
	User addPermissionsOnRole(HasPermissions sourceNode, GraphPermission permission, MeshVertex targetNode, GraphPermission... toGrant);

	/**
	 * Inherit permissions egdes from the source node and assign those permissions to the target node.
	 *
	 * @param sourceNode
	 * @param targetNode
	 * @return Fluent API
	 */
	User inheritRolePermissions(MeshVertex sourceNode, MeshVertex targetNode);

	/**
	 * Same as {@link User#update(InternalActionContext, EventQueueBatch)}, but does not actually perform any changes.
	 *
	 * Useful to check if any changes have to be made.
	 *
	 * @param ac
	 * @return true if the user would have been modified
	 */
	boolean updateDry(InternalActionContext ac);

	/**
	 * Return a page of groups which the user was assigned to.
	 *
	 * @param user
	 * @param params
	 * @return
	 */
	Page<? extends Group> getGroups(User user, PagingParameters params);

	/**
	 * Return a traversal result of groups to which the user was assigned.
	 *
	 * @return
	 */
	TraversalResult<? extends Group> getGroups();

	/**
	 * Add the user to the given group.
	 *
	 * @param group
	 * @return Fluent API
	 */
	User addGroup(Group group);

	/**
	 * A CRC32 hash of the users {@link #getRoles roles}.
	 *
	 * @return A hash of the users roles
	 */
	String getRolesHash();

	/**
	 * Return an iterable of roles which belong to this user. Internally this will fetch all groups of the user and collect the assigned roles.
	 *
	 * @return
	 */
	Iterable<? extends Role> getRoles();

	/**
	 * Return an iterable of roles that belong to the user. Internally this will check the user role shortcut edge.
	 *
	 * @return
	 */
	Iterable<? extends Role> getRolesViaShortcut();

	/**
	 * Return a page of roles which the user was assigned to.
	 *
	 * @param user
	 * @param params
	 * @return
	 */
	Page<? extends Role> getRolesViaShortcut(User user, PagingParameters params);

	/**
	 * Update all shortcut edges.
	 */
	void updateShortcutEdges();

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
	 *
	 * @return
	 */
	User deactivate();

	/**
	 * Check the read permission on the given container and return false if the needed permission to read the container is not set.
	 * This method will not return false if the user has READ permission or READ_PUBLISH permission on a published node.
	 *
	 * @param container
	 * @param branchUuid
	 * @param requestedVersion
	 */
	boolean hasReadPermission(NodeGraphFieldContainer container, String branchUuid, String requestedVersion);

	/**
	 * Check the read permission on the given container and fail if the needed permission to read the container is not set. This method will not fail if the
	 * user has READ permission or READ_PUBLISH permission on a published node.
	 *
	 * @param container
	 * @param branchUuid
	 * @param requestedVersion
	 */
	default void failOnNoReadPermission(NodeGraphFieldContainer container, String branchUuid, String requestedVersion) {
		Node node = container.getParentNode();
		if (!hasReadPermission(container, branchUuid, requestedVersion)) {
			throw error(FORBIDDEN, "error_missing_perm", node.getUuid(),
				"published".equals(requestedVersion)
					? READ_PUBLISHED_PERM.getRestPerm().getName()
					: READ_PERM.getRestPerm().getName());
		}
	}

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
	 * Return true if the user needs to change their password on next login.
	 * 
	 * @return
	 */
	boolean isForcedPasswordChange();

	/**
	 * Set whether the user needs to change their password on next login.
	 * 
	 * @param force
	 * @return
	 */
	User setForcedPasswordChange(boolean force);

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
	 * @return Fluent API
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
		Long resetTokenIssueTimestamp = getResetTokenIssueTimestamp();
		if (token == null || resetTokenIssueTimestamp == null) {
			return false;
		}
		long maxTokenAge = 1000 * 60 * maxTokenAgeMins;
		long tokenAge = System.currentTimeMillis() - resetTokenIssueTimestamp;
		boolean isExpired = tokenAge > maxTokenAge;
		boolean isTokenMatch = token.equals(getResetToken());

		if (isTokenMatch && isExpired) {
			invalidateResetToken();
			return false;
		}
		return isTokenMatch && !isExpired;
	}

	/**
	 * Return the currently stored API token id.
	 *
	 * @return API token id or null if no token has yet been generated.
	 */
	default String getAPIKeyTokenCode() {
		return property(API_TOKEN_ID);
	}

	/**
	 * Set the user API token id.
	 *
	 * @param code
	 * @return Fluent API
	 */
	default User setAPITokenId(String code) {
		property(API_TOKEN_ID, code);
		return this;
	}

	/**
	 * Return the timestamp when the api key token code was last issued.
	 *
	 * @return
	 */
	default Long getAPITokenIssueTimestamp() {
		return property(API_TOKEN_ISSUE_TIMESTAMP);
	}

	/**
	 * Set the API token issue timestamp to the current time.
	 *
	 * @return Fluent API
	 */
	default User setAPITokenIssueTimestamp() {
		setAPITokenIssueTimestamp(System.currentTimeMillis());
		return this;
	}

	/**
	 * Set the API token issue timestamp.
	 *
	 * @param timestamp
	 * @return Fluent API
	 */
	default User setAPITokenIssueTimestamp(Long timestamp) {
		property(API_TOKEN_ISSUE_TIMESTAMP, timestamp);
		return this;
	}

	/**
	 * Return the API token issue date.
	 *
	 * @return ISO8601 formatted date or null if the date has not yet been set
	 */
	default String getAPITokenIssueDate() {
		Long timestamp = getAPITokenIssueTimestamp();
		if (timestamp == null) {
			return null;
		}
		return DateUtils.toISO8601(timestamp, System.currentTimeMillis());
	}

	/**
	 * Reset the API token id and issue timestamp and thus invalidating the token.
	 */
	default void resetAPIToken() {
		setProperty(API_TOKEN_ID, null);
		setProperty(API_TOKEN_ISSUE_TIMESTAMP, null);
	}

	/**
	 * Transform the user to a {@link MeshAuthUser}.
	 * 
	 * @return
	 */
	MeshAuthUser toAuthUser();

	/**
	 * Return the admin flag of the user.
	 * 
	 * @return
	 */
	boolean isAdmin();

	/**
	 * Set the admin flag for the user.
	 * 
	 * @param flag
	 */
	void setAdmin(boolean flag);

}
