package com.gentics.mesh.core.data;

import java.util.List;
import java.util.Objects;

import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Graph domain model interface for groups.
 */
public interface Group extends MeshCoreVertex<GroupResponse, Group>, ReferenceableElement<GroupReference>, UserTrackingVertex, IndexableElement {

	/**
	 * Type Value: {@value #TYPE}
	 */
	static final String TYPE = "group";

	@Override
	default String getType() {
		return Group.TYPE;
	}

	/**
	 * Compose the index name for the group index.
	 * 
	 * @return
	 */
	static String composeIndexName() {
		return Group.TYPE.toLowerCase();
	}

	/**
	 * Compose the document id for the group index.
	 * 
	 * @param groupUuid
	 * @return
	 */
	static String composeDocumentId(String groupUuid) {
		Objects.requireNonNull(groupUuid, "A groupUuid must be provided.");
		return groupUuid;
	}

	/**
	 * Compose the index type for the group index.
	 * 
	 * @return
	 */
	static String composeIndexType() {
		return Group.TYPE.toLowerCase();
	}

	/**
	 * Assign the given user to this group.
	 * 
	 * @param user
	 */
	void addUser(User user);

	/**
	 * Unassign the user from the group.
	 * 
	 * @param user
	 */
	void removeUser(User user);

	/**
	 * Assign the given role to this group.
	 * 
	 * @param role
	 */
	void addRole(Role role);

	/**
	 * Unassign the role from this group.
	 * 
	 * @param role
	 */
	void removeRole(Role role);

	/**
	 * Return a list of users that are assigned to the group.
	 * 
	 * @return
	 */
	List<? extends User> getUsers();

	/**
	 * Return the a list of roles that are assigned to the group.
	 * 
	 * @return
	 */
	List<? extends Role> getRoles();

	/**
	 * Check whether the user has been assigned to the group.
	 * 
	 * @param user
	 * @return
	 */
	boolean hasUser(User user);

	/**
	 * Check whether the role has been assigned to the group.
	 * 
	 * @param role
	 * @return
	 */
	boolean hasRole(Role role);

	/**
	 * Return a page with all visible roles that the given user can see.
	 * 
	 * @param User
	 *            user User which requested the resource
	 * @param pagingInfo
	 *            Paging information
	 * @return Page which contains the retrieved items
	 */
	Page<? extends Role> getRoles(User user, PagingParameters pagingInfo);

	/**
	 * Return a page with all users that the given user can see.
	 * 
	 * @param requestUser
	 * @param pagingInfo
	 * @return Page with found users, an empty page is returned when no users could be found
	 */
	Page<? extends User> getVisibleUsers(MeshAuthUser requestUser, PagingParameters pagingInfo);

}
