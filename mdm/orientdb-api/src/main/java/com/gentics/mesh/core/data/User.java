package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.search.GraphDBBucketableElement;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.PagingParameters;

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
public interface User extends MeshCoreVertex<UserResponse>, ReferenceableElement<UserReference>, UserTrackingVertex, HibUser, GraphDBBucketableElement {

	/**
	 * API token id property name {@value #API_TOKEN_ID}
	 */
	String API_TOKEN_ID = "APITokenId";

	/**
	 * API token timestamp property name {@value #API_TOKEN_ISSUE_TIMESTAMP}
	 */
	String API_TOKEN_ISSUE_TIMESTAMP = "APITokenTimestamp";

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
	 * Return the timestamp when the api key token code was last issued.
	 *
	 * @return
	 */
	default Long getAPITokenIssueTimestamp() {
		return property(API_TOKEN_ISSUE_TIMESTAMP);
	}

	/**
	 * Set the API token issue timestamp.
	 *
	 * @param timestamp
	 * @return Fluent API
	 */
	HibUser setAPITokenIssueTimestamp(Long timestamp);

	/**
	 * Reset the API token id and issue timestamp and thus invalidating the token.
	 */
	default void resetAPIToken() {
		setProperty(API_TOKEN_ID, null);
		setProperty(API_TOKEN_ISSUE_TIMESTAMP, null);
	}
}
