package com.gentics.mesh.core.data.user;

import static com.gentics.mesh.core.rest.MeshEvent.USER_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_UPDATED;

import java.util.Objects;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.BucketableElement;
import com.gentics.mesh.core.data.CoreElement;
import com.gentics.mesh.core.data.NamedBaseElement;
import com.gentics.mesh.core.data.ReferenceableElement;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.user.UserReferenceModel;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.handler.VersionUtils;
import com.gentics.mesh.util.DateUtils;

/**
 * Domain model for user.
 */
public interface User extends CoreElement<UserResponse>, ReferenceableElement<UserReferenceModel>, UserTracking, BucketableElement, NamedBaseElement {

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
	 * Return the password hash.
	 *
	 * @return Password hash
	 */
	String getPasswordHash();

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
	 * Transform the user to a reference POJO.
	 * 
	 * @return
	 */
	UserReferenceModel transformToReference();

	/**
	 * Return the timestamp at which the reset token was issued.
	 * 
	 * @return
	 */
	Long getResetTokenIssueTimestamp();

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
	 * Set the password hash.
	 * 
	 * @param hash
	 * @return
	 */
	User setPasswordHash(String hash);

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
	User setResetToken(String token);

	/**
	 * Set the user API token id.
	 *
	 * @param code
	 * @return Fluent API
	 */
	User setAPITokenId(String code);

	/**
	 * Set the token code issue timestamp. This is used to influence the token expire moment.
	 *
	 * @param timestamp
	 * @return Fluent API
	 */
	User setResetTokenIssueTimestamp(Long timestamp);

	/**
	 * Set the API token issue timestamp to the current time.
	 *
	 * @return Fluent API
	 */
	User setAPITokenIssueTimestamp();

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
	 * Return the currently stored reset token.
	 *
	 * @return Token or null if no token has been set or if the token has been used up
	 */
	String getResetToken();

	// Legacy - compat stuff

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
	 * Transform the user to a {@link MeshAuthUser} which implements the {@link User} interface and is thus usable in Vert.x Auth API code.
	 * 
	 * @return
	 */
	MeshAuthUser toAuthUser();

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return VersionUtils.baseRoute(ac) + "/users/" + getUuid();
	}

	@Override
	default String getSubETag(InternalActionContext ac) {
		UserDao userDao = Tx.get().userDao();
		return userDao.getSubETag(this, ac);
	}
}
