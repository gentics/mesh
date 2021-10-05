package com.gentics.mesh.core.data.user;

import static com.gentics.mesh.core.rest.MeshEvent.USER_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_UPDATED;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.handler.VersionUtils;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.DateUtils;

import io.vertx.ext.auth.User;


/**
 * Domain model for user.
 */
public interface HibUser extends HibCoreElement<UserResponse>, HibUserTracking, HibBucketableElement, HibNamedElement {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.USER, USER_CREATED, USER_UPDATED, USER_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
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
	HibUser setLastname(String lastname);

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
	HibUser setFirstname(String firstname);

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
	HibUser setEmailAddress(String email);

	/**
	 * Return the password hash.
	 *
	 * @return Password hash
	 */
	String getPasswordHash();

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
	 * Transform the user to a reference POJO.
	 * 
	 * @return
	 */
	UserReference transformToReference();

	/**
	 * Return the timestamp at which the reset token was issued.
	 * 
	 * @return
	 */
	Long getResetTokenIssueTimestamp();

	/**
	 * Set the password hash.
	 * 
	 * @param hash
	 * @return
	 */
	HibUser setPasswordHash(String hash);

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

	/**
	 * Return the currently stored API token id.
	 *
	 * @return API token id or null if no token has yet been generated.
	 */
	String getAPIKeyTokenCode();

	/**
	 * Return the timestamp when the api key token code was last issued.
	 *
	 * @return
	 */
	Long getAPITokenIssueTimestamp();

	/**
	 * Return an iterable of roles which belong to this user. Internally this will fetch all groups of the user and collect the assigned roles.
	 *
	 * @return
	 */
	// TODO we want to use this to the user dao
	// Iterable<? extends Role> getRoles();

	/**
	 * Reset the API token id and issue timestamp and thus invalidating the token.
	 */
	void resetAPIToken();

	/**
	 * Set the reset token for the user.
	 *
	 * @param token
	 * @return Fluent API
	 */
	HibUser setResetToken(String token);

	/**
	 * Set the user API token id.
	 *
	 * @param code
	 * @return Fluent API
	 */
	HibUser setAPITokenId(String code);

	/**
	 * Set the token code issue timestamp. This is used to influence the token expire moment.
	 *
	 * @param timestamp
	 * @return Fluent API
	 */
	HibUser setResetTokenIssueTimestamp(Long timestamp);

	/**
	 * Set the API token issue timestamp to the current time.
	 *
	 * @return Fluent API
	 */
	HibUser setAPITokenIssueTimestamp();

	/**
	 * Return the currently stored reset token.
	 *
	 * @return Token or null if no token has been set or if the token has been used up
	 */
	String getResetToken();

	// Legacy - compat stuff

	/**
	 * Update all shortcut edges.
	 */
	void updateShortcutEdges();

	/**
	 * Return the referenced node which was assigned to the user.
	 *
	 * @return Referenced node or null when no node was assigned to the user.
	 */
	HibNode getReferencedNode();

	/**
	 * Set the referenced node.
	 *
	 * @param node
	 * @return Fluent API
	 */
	HibUser setReferencedNode(HibNode node);

	/**
	 * A CRC32 hash of the users {@link #getRoles roles}.
	 *
	 * @return A hash of the users roles
	 */
	String getRolesHash();

	/**
	 * Return the current element version.
	 * 
	 * TODO: Check how versions can be accessed via Hibernate and refactor / remove this method accordingly
	 * 
	 * @return
	 */
	String getElementVersion();

	/**
	 * Transform the user to a {@link MeshAuthUser} which implements the {@link User} interface and is thus usable in Vert.x Auth API code.
	 * 
	 * @return
	 */
	MeshAuthUser toAuthUser();

	/**
	 * Return a page of groups which the user was assigned to.
	 *
	 * @param user
	 * @param params
	 * @return
	 */
	Page<? extends HibGroup> getGroups(HibUser user, PagingParameters params);

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
	HibUser addGroup(HibGroup group);

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

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return VersionUtils.baseRoute(ac) + "/users/" + getUuid();
	}

	@Deprecated
	@Override
	default UserResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		UserDao userDao = Tx.get().userDao();
		return userDao.transformToRestSync(this, ac, level, languageTags);
	}
}
