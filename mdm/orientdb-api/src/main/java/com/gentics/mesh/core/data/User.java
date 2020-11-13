package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.rest.MeshEvent.USER_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_UPDATED;

import java.util.Objects;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.search.BucketableElement;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.result.Result;
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
public interface User extends MeshCoreVertex<UserResponse>, ReferenceableElement<UserReference>, UserTrackingVertex, HibUser, BucketableElement {

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
	HibUser setUsername(String string);

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
	HibUser setPasswordHash(String hash);

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
	HibUser setReferencedNode(HibNode node);

	/**
	 * Return a page of groups which the user was assigned to.
	 *
	 * @param user
	 * @param params
	 * @return
	 */
	Page<? extends Group> getGroups(HibUser user, PagingParameters params);

	/**
	 * Return a traversal result of groups to which the user was assigned.
	 *
	 * @return
	 */
	Result<? extends HibGroup> getGroups();

	/**
	 * Add the user to the given group.
	 *
	 * @param group
	 * @return Fluent API
	 */
	HibUser addGroup(Group group);

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
	Iterable<? extends HibRole> getRoles();

	/**
	 * Return an iterable of roles that belong to the user. Internally this will check the user role shortcut edge.
	 *
	 * @return
	 */
	Iterable<? extends HibRole> getRolesViaShortcut();

	/**
	 * Return a page of roles which the user was assigned to.
	 *
	 * @param user
	 * @param params
	 * @return
	 */
	Page<? extends HibRole> getRolesViaShortcut(HibUser user, PagingParameters params);

	/**
	 * Update all shortcut edges.
	 */
	void updateShortcutEdges();

	/**
	 * Disable the user.
	 */
	HibUser disable();

	/**
	 * Check whether the user is enabled.
	 *
	 * @return
	 */
	boolean isEnabled();

	/**
	 * Enable the user.
	 */
	HibUser enable();

	/**
	 * Set the reset token for the user.
	 *
	 * @param token
	 * @return Fluent API
	 */
	HibUser setResetToken(String token);

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
	HibUser setForcedPasswordChange(boolean force);

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
	HibUser setResetTokenIssueTimestamp(Long timestamp);

	/**
	 * Invalidate the reset token.
	 *
	 * @return Fluent API
	 */
	default HibUser invalidateResetToken() {
		setResetToken(null);
		setResetTokenIssueTimestamp(null);
		return this;
	}

	/**
	 * Return the currently stored API token id.
	 *
	 * @return API token id or null if no token has yet been generated.
	 */
	String getAPIKeyTokenCode();

	/**
	 * Set the user API token id.
	 *
	 * @param code
	 * @return Fluent API
	 */
	HibUser setAPITokenId(String code);

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
	HibUser setAPITokenIssueTimestamp();

	/**
	 * Set the API token issue timestamp.
	 *
	 * @param timestamp
	 * @return Fluent API
	 */
	HibUser setAPITokenIssueTimestamp(Long timestamp);

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
